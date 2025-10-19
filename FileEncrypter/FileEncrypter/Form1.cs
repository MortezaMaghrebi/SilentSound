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



        int result_success = 0;
        int result_error = 0;
        string filenames = "";
        string filenames_enc = "";
        void EncryptRecursive(String current_directory,String target_directory)
        {
            if (!Directory.Exists(target_directory))
            {
                Directory.CreateDirectory(target_directory);
            }
            string[] filetypes = txtFileTypes.Text.Split(',');
            foreach (string filetype in filetypes)
            {
                string[] files = Directory.GetFiles(current_directory, filetype);
                for (int i = 0; i < files.Length; i++)
                {
                    try
                    {
                        string file = files[i];
                        string fileNameWithoutExt = Path.GetFileNameWithoutExtension(file);
                        string formattedFileNameWithoutExt = fileNameWithoutExt.Replace("-", "_").Replace(" ", "_");
                        string target_file_name_without_ext = chbEncryptFileNames.Checked ? Encrypter.EncodeString(formattedFileNameWithoutExt) : formattedFileNameWithoutExt;
                        string extension = Path.GetExtension(file);
                        string target_file_name = Path.Combine(target_directory, $"{target_file_name_without_ext}{extension}");
                        bool result = Encrypter.EncryptFile(file, target_file_name, txtPassword.Text.Trim());
                        if (result)
                        {
                            result_success++;
                            filenames += file.Replace(txtInputFolder.Text,"").Replace("-", "_").Replace(" ", "_") + "\r\n";
                            filenames_enc += target_file_name.Replace(txtOutputFolder.Text, "") + "\r\n";
                        }
                        else result_error++;
                        progress_Files.Value = (i + 1) * 100 / files.Length;
                        progress_Files.Invalidate();
                        Application.DoEvents();
                    }
                    catch (Exception ex)
                    {
                        result_error++;
                    }
                }
            }

            string[] directories = Directory.GetDirectories(current_directory);
            for (int i = 0; i < directories.Length; i++)
            {
                try
                {
                    string directory = directories[i];
                    string sub_directory_name = Path.GetFileName(directory);
                    if (sub_directory_name.Equals(".git")) continue;
                    string target_sub_directory_name = chbEncryptFolderNames.Checked? Encrypter.EncodeString(sub_directory_name): sub_directory_name;
                    string target_sub_directory = Path.Combine(target_directory, target_sub_directory_name);
                    string current_sub_directory = Path.Combine(current_directory, sub_directory_name);
                    EncryptRecursive(current_sub_directory, target_sub_directory);
                    progress_Folders.Value = (i + 1) * 100 / directories.Length;
                    progress_Folders.Invalidate();
                    Application.DoEvents();
                }
                catch (Exception ex)
                {
                    // لاگ خطا
                    Console.WriteLine($"Error processing directory {directories[i]}: {ex.Message}");
                }
            }


        }



        private void btnEncrypt_Click(object sender, EventArgs e)
        {
            result_success = 0;
            result_error = 0;
            filenames = "";
            filenames_enc = "";
            if (Directory.Exists(txtInputFolder.Text) && Directory.Exists(txtOutputFolder.Text))
            {
                EncryptRecursive(txtInputFolder.Text, txtOutputFolder.Text);
                System.IO.File.WriteAllText(Path.Combine(txtOutputFolder.Text,"FILENAMES.txt"),filenames);
                System.IO.File.WriteAllText(Path.Combine(txtOutputFolder.Text,"FILENAMES_ENC.txt"), filenames_enc);
                MessageBox.Show("Encryption Finished!\nSuccess: " + result_success + "\nFail: " + result_error);
            }else
            {
                MessageBox.Show("Please select correct input and output directories!");
            }
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            
        }

        private void panel1_Resize(object sender, EventArgs e)
        {
            txtFileTypes.Width = Width + 512 - 616;
            txtPassword.Width = Width + 512 - 616;
        }

        private void btnBrowseOutputFolder_Click(object sender, EventArgs e)
        {
            SaveFileDialog saveFileDialog = new SaveFileDialog();
            saveFileDialog.Filter = "*.folder|*.folder";
            saveFileDialog.FileName = "Select This ";
            if(saveFileDialog.ShowDialog()==DialogResult.OK)
            {
                txtOutputFolder.Text = Path.GetDirectoryName(saveFileDialog.FileName);
            }
        }

        private void btnBrowseInputFolder_Click(object sender, EventArgs e)
        {
            SaveFileDialog saveFileDialog = new SaveFileDialog();
            saveFileDialog.Filter = "*.folder|*.folder";
            saveFileDialog.FileName = "Select This ";
            if (saveFileDialog.ShowDialog() == DialogResult.OK)
            {
                txtInputFolder.Text = Path.GetDirectoryName(saveFileDialog.FileName);
            }
        }
    }
}
