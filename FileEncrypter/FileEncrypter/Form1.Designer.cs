namespace FileEncrypter
{
    partial class Form1
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.lblPath = new System.Windows.Forms.Label();
            this.btnEncrypt = new System.Windows.Forms.Button();
            this.label1 = new System.Windows.Forms.Label();
            this.panel1 = new System.Windows.Forms.Panel();
            this.btnBrowseInputFolder = new System.Windows.Forms.Button();
            this.txtInputFolder = new System.Windows.Forms.TextBox();
            this.label5 = new System.Windows.Forms.Label();
            this.btnBrowseOutputFolder = new System.Windows.Forms.Button();
            this.txtOutputFolder = new System.Windows.Forms.TextBox();
            this.label4 = new System.Windows.Forms.Label();
            this.chbEncryptFolderNames = new System.Windows.Forms.CheckBox();
            this.chbEncryptFileNames = new System.Windows.Forms.CheckBox();
            this.label3 = new System.Windows.Forms.Label();
            this.txtPassword = new System.Windows.Forms.TextBox();
            this.label2 = new System.Windows.Forms.Label();
            this.txtFileTypes = new System.Windows.Forms.TextBox();
            this.progress_Folders = new System.Windows.Forms.ProgressBar();
            this.progress_Files = new System.Windows.Forms.ProgressBar();
            this.panel1.SuspendLayout();
            this.SuspendLayout();
            // 
            // lblPath
            // 
            this.lblPath.AutoSize = true;
            this.lblPath.Location = new System.Drawing.Point(44, 58);
            this.lblPath.Margin = new System.Windows.Forms.Padding(2, 0, 2, 0);
            this.lblPath.Name = "lblPath";
            this.lblPath.Size = new System.Drawing.Size(0, 13);
            this.lblPath.TabIndex = 1;
            // 
            // btnEncrypt
            // 
            this.btnEncrypt.Dock = System.Windows.Forms.DockStyle.Top;
            this.btnEncrypt.Font = new System.Drawing.Font("Microsoft Sans Serif", 12F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.btnEncrypt.Location = new System.Drawing.Point(0, 337);
            this.btnEncrypt.Margin = new System.Windows.Forms.Padding(2);
            this.btnEncrypt.Name = "btnEncrypt";
            this.btnEncrypt.Size = new System.Drawing.Size(600, 79);
            this.btnEncrypt.TabIndex = 3;
            this.btnEncrypt.Text = "Encrypt";
            this.btnEncrypt.UseVisualStyleBackColor = true;
            this.btnEncrypt.Click += new System.EventHandler(this.btnEncrypt_Click);
            // 
            // label1
            // 
            this.label1.Dock = System.Windows.Forms.DockStyle.Top;
            this.label1.Font = new System.Drawing.Font("Microsoft Sans Serif", 9.75F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label1.ForeColor = System.Drawing.Color.Navy;
            this.label1.Location = new System.Drawing.Point(0, 0);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(600, 67);
            this.label1.TabIndex = 4;
            this.label1.Text = "Copy exe file of this application to a folder, this application will encrypt all " +
    "files in that folder and subfolders with selected tpes";
            this.label1.TextAlign = System.Drawing.ContentAlignment.MiddleCenter;
            // 
            // panel1
            // 
            this.panel1.Controls.Add(this.btnBrowseInputFolder);
            this.panel1.Controls.Add(this.txtInputFolder);
            this.panel1.Controls.Add(this.label5);
            this.panel1.Controls.Add(this.btnBrowseOutputFolder);
            this.panel1.Controls.Add(this.txtOutputFolder);
            this.panel1.Controls.Add(this.label4);
            this.panel1.Controls.Add(this.chbEncryptFolderNames);
            this.panel1.Controls.Add(this.chbEncryptFileNames);
            this.panel1.Controls.Add(this.label3);
            this.panel1.Controls.Add(this.txtPassword);
            this.panel1.Controls.Add(this.label2);
            this.panel1.Controls.Add(this.txtFileTypes);
            this.panel1.Dock = System.Windows.Forms.DockStyle.Top;
            this.panel1.Location = new System.Drawing.Point(0, 67);
            this.panel1.Name = "panel1";
            this.panel1.Size = new System.Drawing.Size(600, 270);
            this.panel1.TabIndex = 5;
            this.panel1.Resize += new System.EventHandler(this.panel1_Resize);
            // 
            // btnBrowseInputFolder
            // 
            this.btnBrowseInputFolder.Location = new System.Drawing.Point(92, 119);
            this.btnBrowseInputFolder.Name = "btnBrowseInputFolder";
            this.btnBrowseInputFolder.Size = new System.Drawing.Size(90, 23);
            this.btnBrowseInputFolder.TabIndex = 11;
            this.btnBrowseInputFolder.Text = "Browse";
            this.btnBrowseInputFolder.UseVisualStyleBackColor = true;
            this.btnBrowseInputFolder.Click += new System.EventHandler(this.btnBrowseInputFolder_Click);
            // 
            // txtInputFolder
            // 
            this.txtInputFolder.Location = new System.Drawing.Point(15, 152);
            this.txtInputFolder.Name = "txtInputFolder";
            this.txtInputFolder.Size = new System.Drawing.Size(573, 20);
            this.txtInputFolder.TabIndex = 10;
            // 
            // label5
            // 
            this.label5.AutoSize = true;
            this.label5.Location = new System.Drawing.Point(12, 124);
            this.label5.Name = "label5";
            this.label5.Size = new System.Drawing.Size(66, 13);
            this.label5.TabIndex = 9;
            this.label5.Text = "Input Folder:";
            // 
            // btnBrowseOutputFolder
            // 
            this.btnBrowseOutputFolder.Location = new System.Drawing.Point(92, 189);
            this.btnBrowseOutputFolder.Name = "btnBrowseOutputFolder";
            this.btnBrowseOutputFolder.Size = new System.Drawing.Size(90, 23);
            this.btnBrowseOutputFolder.TabIndex = 8;
            this.btnBrowseOutputFolder.Text = "Browse";
            this.btnBrowseOutputFolder.UseVisualStyleBackColor = true;
            this.btnBrowseOutputFolder.Click += new System.EventHandler(this.btnBrowseOutputFolder_Click);
            // 
            // txtOutputFolder
            // 
            this.txtOutputFolder.Location = new System.Drawing.Point(15, 222);
            this.txtOutputFolder.Name = "txtOutputFolder";
            this.txtOutputFolder.Size = new System.Drawing.Size(573, 20);
            this.txtOutputFolder.TabIndex = 7;
            // 
            // label4
            // 
            this.label4.AutoSize = true;
            this.label4.Location = new System.Drawing.Point(12, 194);
            this.label4.Name = "label4";
            this.label4.Size = new System.Drawing.Size(74, 13);
            this.label4.TabIndex = 6;
            this.label4.Text = "Output Folder:";
            // 
            // chbEncryptFolderNames
            // 
            this.chbEncryptFolderNames.AutoSize = true;
            this.chbEncryptFolderNames.Checked = true;
            this.chbEncryptFolderNames.CheckState = System.Windows.Forms.CheckState.Checked;
            this.chbEncryptFolderNames.Location = new System.Drawing.Point(146, 74);
            this.chbEncryptFolderNames.Name = "chbEncryptFolderNames";
            this.chbEncryptFolderNames.Size = new System.Drawing.Size(130, 17);
            this.chbEncryptFolderNames.TabIndex = 5;
            this.chbEncryptFolderNames.Text = "Encrypt Folder Names";
            this.chbEncryptFolderNames.UseVisualStyleBackColor = true;
            // 
            // chbEncryptFileNames
            // 
            this.chbEncryptFileNames.AutoSize = true;
            this.chbEncryptFileNames.Checked = true;
            this.chbEncryptFileNames.CheckState = System.Windows.Forms.CheckState.Checked;
            this.chbEncryptFileNames.Location = new System.Drawing.Point(15, 74);
            this.chbEncryptFileNames.Name = "chbEncryptFileNames";
            this.chbEncryptFileNames.Size = new System.Drawing.Size(112, 17);
            this.chbEncryptFileNames.TabIndex = 4;
            this.chbEncryptFileNames.Text = "Encrypt File Name";
            this.chbEncryptFileNames.UseVisualStyleBackColor = true;
            // 
            // label3
            // 
            this.label3.AutoSize = true;
            this.label3.Location = new System.Drawing.Point(12, 42);
            this.label3.Name = "label3";
            this.label3.Size = new System.Drawing.Size(56, 13);
            this.label3.TabIndex = 3;
            this.label3.Text = "Password:";
            // 
            // txtPassword
            // 
            this.txtPassword.Location = new System.Drawing.Point(76, 39);
            this.txtPassword.Name = "txtPassword";
            this.txtPassword.PasswordChar = '*';
            this.txtPassword.Size = new System.Drawing.Size(512, 20);
            this.txtPassword.TabIndex = 2;
            this.txtPassword.Text = "sound113355";
            // 
            // label2
            // 
            this.label2.AutoSize = true;
            this.label2.Location = new System.Drawing.Point(12, 16);
            this.label2.Name = "label2";
            this.label2.Size = new System.Drawing.Size(58, 13);
            this.label2.TabIndex = 1;
            this.label2.Text = "File Types:";
            // 
            // txtFileTypes
            // 
            this.txtFileTypes.Location = new System.Drawing.Point(76, 13);
            this.txtFileTypes.Name = "txtFileTypes";
            this.txtFileTypes.Size = new System.Drawing.Size(512, 20);
            this.txtFileTypes.TabIndex = 0;
            this.txtFileTypes.Text = "*.mp3";
            // 
            // progress_Folders
            // 
            this.progress_Folders.Dock = System.Windows.Forms.DockStyle.Top;
            this.progress_Folders.Location = new System.Drawing.Point(0, 416);
            this.progress_Folders.Name = "progress_Folders";
            this.progress_Folders.Size = new System.Drawing.Size(600, 10);
            this.progress_Folders.TabIndex = 6;
            // 
            // progress_Files
            // 
            this.progress_Files.Dock = System.Windows.Forms.DockStyle.Top;
            this.progress_Files.Location = new System.Drawing.Point(0, 426);
            this.progress_Files.Name = "progress_Files";
            this.progress_Files.Size = new System.Drawing.Size(600, 10);
            this.progress_Files.TabIndex = 7;
            // 
            // Form1
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(600, 540);
            this.Controls.Add(this.progress_Files);
            this.Controls.Add(this.progress_Folders);
            this.Controls.Add(this.btnEncrypt);
            this.Controls.Add(this.lblPath);
            this.Controls.Add(this.panel1);
            this.Controls.Add(this.label1);
            this.Margin = new System.Windows.Forms.Padding(2);
            this.Name = "Form1";
            this.Text = "File Encryptor";
            this.Load += new System.EventHandler(this.Form1_Load);
            this.panel1.ResumeLayout(false);
            this.panel1.PerformLayout();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion
        private System.Windows.Forms.Label lblPath;
        private System.Windows.Forms.Button btnEncrypt;
        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.Panel panel1;
        private System.Windows.Forms.Label label3;
        private System.Windows.Forms.TextBox txtPassword;
        private System.Windows.Forms.Label label2;
        private System.Windows.Forms.TextBox txtFileTypes;
        private System.Windows.Forms.CheckBox chbEncryptFolderNames;
        private System.Windows.Forms.CheckBox chbEncryptFileNames;
        private System.Windows.Forms.Button btnBrowseOutputFolder;
        private System.Windows.Forms.TextBox txtOutputFolder;
        private System.Windows.Forms.Label label4;
        private System.Windows.Forms.ProgressBar progress_Folders;
        private System.Windows.Forms.ProgressBar progress_Files;
        private System.Windows.Forms.Button btnBrowseInputFolder;
        private System.Windows.Forms.TextBox txtInputFolder;
        private System.Windows.Forms.Label label5;
    }
}

