using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;

namespace FileEncrypter
{
    internal class Encrypter
    {
        public static bool EncryptFile(string inputPath,string outputPath, string password)
        {
            try
            {
                if (!File.Exists(inputPath))
                    throw new FileNotFoundException("فایل مورد نظر یافت نشد.");

                // تولید salt تصادفی
                byte[] salt = GenerateRandomSalt();

                // تولید key و IV از پسورد
                using (var key = new Rfc2898DeriveBytes(password, salt, 10000, HashAlgorithmName.SHA256))
                {
                    byte[] keyBytes = key.GetBytes(32); // 256-bit key
                    byte[] iv = key.GetBytes(16);       // 128-bit IV

                    // نام فایل خروجی
                    string directory = Path.GetDirectoryName(inputPath);
                    //string fileNameWithoutExt = Path.GetFileNameWithoutExtension(filename);
                    //string extension = Path.GetExtension(filename);
                    //string outputFilename = Path.Combine(directory, $"{fileNameWithoutExt}_enc{extension}");
                    string outputFilename = outputPath;

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
                            using (var fsInput = new FileStream(inputPath, FileMode.Open))
                            {
                                // کپی و انکریپت داده‌ها
                                fsInput.CopyTo(cryptoStream);
                            }
                        }
                    }
                    Console.WriteLine($"Encrypted successfully: {outputFilename}");
                    return true;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error in Encryption: {ex.Message}");
                return false;
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

        public static string EncodeString(String input)
        {
            string alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
            string map = "zuPTUVWXNOopixyEAeRntlmwSghBDaJFYvjkKCfQbcdrsLMGHIqZ";

            string output = "";
            for(int i=0;i<input.Length;i++)
            {
                char char_i = input[i];
                if (i % 2 == 0)
                {
                    output += char_i;
                    continue;
                }
                bool find = false;
                for(int j=0;j<alphabet.Length;j++)
                {
                    char letter = alphabet[j];
                    if (char_i == letter)
                    {
                        output += map[j];
                        find = true;
                        break;
                    }
                }
                if (!find) output += char_i;
            }
            return output;
        }
        public static string DecodeString(String input)
        {
            string alphabet = "zuPTUVWXNOopixyEAeRntlmwSghBDaJFYvjkKCfQbcdrsLMGHIqZ";
            string map = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

            string output = "";
            for (int i = 0; i < input.Length; i++)
            {
                char char_i = input[i];
                if (i % 2 == 0)
                {
                    output += char_i;
                    continue;
                }
                bool find = false;
                for (int j = 0; j < alphabet.Length; j++)
                {
                    char letter = alphabet[j];
                    if (char_i == letter)
                    {
                        output += map[j];
                        find = true;
                        break;
                    }
                }
                if (!find) output += char_i;
            }
            return output;
        }
    }
}
