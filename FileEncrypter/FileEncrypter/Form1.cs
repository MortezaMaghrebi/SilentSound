using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Security.Cryptography;
using Org.BouncyCastle.Security;
using Org.BouncyCastle.Crypto.Modes;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Crypto.Engines;

namespace FileEncrypter
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();
        }

        private void btnBrowse_Click(object sender, EventArgs e)
        {
            using (OpenFileDialog ofd = new OpenFileDialog())
            {
                ofd.Filter = "Audio Files|*.mp3;*.wav;*.ogg;*.m4a|All Files|*.*";
                if (ofd.ShowDialog() == DialogResult.OK)
                {
                    lblPath.Text= (ofd.FileName);
                }
            }
        }

        private void EncryptFile(string inputFilePath)
        {
            try
            {
                // کلید AES-256 به صورت hex string — این باید همان کلیدی باشد که در اندروید استفاده می‌کنی
                string keyHex = rtxKEY.Text; // به صورت 64 کارکتر hex برای AES-256
                byte[] key = HexStringToBytes(keyHex);
                if (key == null || (key.Length != 32))
                {
                    MessageBox.Show("کلید نامعتبر است. مطمئن شو که یک AES-256 (32 بایت) hex وارد کرده‌ای.", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    return;
                }

                // مسیر خروجی: کنار فایل اصلی و با پسوند _enc (بدون پسوند فایل)
                string outputFilePath = Path.Combine(
                    Path.GetDirectoryName(inputFilePath),
                    Path.GetFileNameWithoutExtension(inputFilePath) + "_enc"
                );

                // خواندن فایل ورودی
                byte[] plaintext = File.ReadAllBytes(inputFilePath);

                // تولید IV تصادفی 12 بایتی (GCM)
                SecureRandom rnd = new SecureRandom();
                byte[] iv = new byte[12];
                rnd.NextBytes(iv);

                // تنظیم GCM با BouncyCastle
                GcmBlockCipher gcm = new GcmBlockCipher(new AesEngine());
                AeadParameters parameters = new AeadParameters(new KeyParameter(key), 128, iv, null); // 128-bit tag
                gcm.Init(true, parameters);

                // خروجی سایز لازم
                byte[] outBuf = new byte[gcm.GetOutputSize(plaintext.Length)];
                int outLen1 = gcm.ProcessBytes(plaintext, 0, plaintext.Length, outBuf, 0);
                int outLen2 = gcm.DoFinal(outBuf, outLen1);
                int totalOut = outLen1 + outLen2;

                // outBuf شامل: [ciphertext || tag] (در این ترتیب)
                int tagLen = 16; // 128-bit tag = 16 bytes
                if (totalOut < tagLen)
                    throw new Exception("نتیجه رمزنگاری نامعتبر است.");

                int cipherTextLen = totalOut - tagLen;

                byte[] ciphertext = new byte[cipherTextLen];
                byte[] tag = new byte[tagLen];

                Array.Copy(outBuf, 0, ciphertext, 0, cipherTextLen);
                Array.Copy(outBuf, cipherTextLen, tag, 0, tagLen);

                // نوشتن به فایل خروجی با فرمت: IV(12) + TAG(16) + CIPHERTEXT
                using (FileStream fs = new FileStream(outputFilePath, FileMode.Create, FileAccess.Write))
                {
                    fs.Write(iv, 0, iv.Length);
                    fs.Write(ciphertext, 0, ciphertext.Length);
                    fs.Write(tag, 0, tag.Length);
                }

                MessageBox.Show($"✅ فایل رمزگذاری شد و ذخیره شد:\n{outputFilePath}", "Done", MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
            catch (Exception ex)
            {
                MessageBox.Show($"❌ خطا در رمزگذاری:\n{ex.Message}", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        private static byte[] HexStringToBytes(string hex)
        {
            if (string.IsNullOrWhiteSpace(hex))
                return null;
            hex = hex.Trim();
            if (hex.StartsWith("0x", StringComparison.OrdinalIgnoreCase))
                hex = hex.Substring(2);
            if (hex.Length % 2 != 0)
                return null;

            byte[] result = new byte[hex.Length / 2];
            for (int i = 0; i < result.Length; i++)
            {
                string byteValue = hex.Substring(i * 2, 2);
                result[i] = Convert.ToByte(byteValue, 16);
            }
            return result;
        }

        private void btnEncrypt_Click(object sender, EventArgs e)
        {
            EncryptFile1(lblPath.Text,"Hello");
        }

        private string GenerateAes256KeyHex()
        {
            byte[] key = new byte[32]; // 256 bits
            using (var rng = new RNGCryptoServiceProvider())
            {
                rng.GetBytes(key);
            }
            // تبدیل به hex کوچک (lowercase)
            string hex = BitConverter.ToString(key).Replace("-", "").ToLowerInvariant();
            return hex;
        }

        // مثال: استفاده و نمایش به کاربر
        private void btnGenerateKey_Click(object sender, EventArgs e)
        {
            string keyHex = GenerateAes256KeyHex();
            // نمایش در MessageBox (برای تست) — در عمل کلید را امن ذخیره کنید
            MessageBox.Show("Generated AES-256 key (hex):\n" + keyHex, "Key Generated", MessageBoxButtons.OK, MessageBoxIcon.Information);

            // یا ذخیره در فایل کنار برنامه (فقط برای تست، نه برای production)
            string path = System.IO.Path.Combine(Application.StartupPath, "aes_key.hex");
            rtxKEY.Text = keyHex;
            System.IO.File.WriteAllText(path, keyHex);
            MessageBox.Show("Key saved to: " + path, "Saved", MessageBoxButtons.OK, MessageBoxIcon.Information);
        }

        public static void EncryptFile1(string filename, string password)
        {
            try
            {
                if (!File.Exists(filename))
                    throw new FileNotFoundException("فایل مورد نظر یافت نشد.");

                // تولید salt تصادفی
                byte[] salt = GenerateRandomSalt();

                // تولید key و IV از پسورد
                using (var key = new Rfc2898DeriveBytes(password, salt, 10000, HashAlgorithmName.SHA256))
                {
                    byte[] keyBytes = key.GetBytes(32); // 256-bit key
                    byte[] iv = key.GetBytes(16);       // 128-bit IV

                    // نام فایل خروجی
                    string directory = Path.GetDirectoryName(filename);
                    string fileNameWithoutExt = Path.GetFileNameWithoutExtension(filename);
                    string extension = Path.GetExtension(filename);
                    string outputFilename = Path.Combine(directory, $"{fileNameWithoutExt}_enc{extension}");

                    using (var fsOutput = new FileStream(outputFilename, FileMode.Create))
                    {
                        // نوشتن salt در ابتدای فایل
                        fsOutput.Write(salt, 0, salt.Length);
                        // نوشتن IV بعد از salt
                        fsOutput.Write(iv, 0, iv.Length);

                        using (var aes = Aes.Create())
                        {
                            aes.Key = keyBytes;
                            aes.IV = iv;
                            aes.Padding = PaddingMode.PKCS7;

                            using (var cryptoStream = new CryptoStream(fsOutput, aes.CreateEncryptor(), CryptoStreamMode.Write))
                            using (var fsInput = new FileStream(filename, FileMode.Open))
                            {
                                // کپی و انکریپت داده‌ها
                                fsInput.CopyTo(cryptoStream);
                            }
                        }
                    }

                    Console.WriteLine($"فایل با موفقیت انکریپت شد: {outputFilename}");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"خطا در انکریپت کردن: {ex.Message}");
                throw;
            }
        }

        private static byte[] GenerateRandomSalt()
        {
            byte[] salt = new byte[32];
            using (var rng = RandomNumberGenerator.Create())
            {
                rng.GetBytes(salt);
            }
            return salt;
        }
    }
}
