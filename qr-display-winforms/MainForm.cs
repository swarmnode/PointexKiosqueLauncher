using System;
using System.Drawing;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace PointexProvisioningQr;

/// <summary>
/// Displays the latest Device Owner provisioning QR code for
/// PointexKioskLauncher, fetched from the GitHub release assets.
/// </summary>
public sealed class MainForm : Form
{
    private const string QrCodeUrl =
        "https://github.com/swarmnode/PointexKiosqueLauncher/releases/latest/download/provisioning_qr.png";

    private readonly PictureBox _pictureBox;
    private readonly Label _statusLabel;

    public MainForm()
    {
        Text = "QR de provisioning - Pointex Kiosk";
        ClientSize = new Size(500, 560);
        FormBorderStyle = FormBorderStyle.FixedDialog;
        MaximizeBox = false;
        StartPosition = FormStartPosition.CenterScreen;

        _pictureBox = new PictureBox
        {
            Dock = DockStyle.Fill,
            SizeMode = PictureBoxSizeMode.Zoom,
        };
        Controls.Add(_pictureBox);

        _statusLabel = new Label
        {
            Dock = DockStyle.Bottom,
            TextAlign = ContentAlignment.MiddleCenter,
            Height = 32,
            Text = "Chargement du QR code...",
        };
        Controls.Add(_statusLabel);

        Load += async (sender, e) => await LoadQrCodeAsync();
    }

    private async Task LoadQrCodeAsync()
    {
        ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;

        try
        {
            using var client = new HttpClient();
            client.DefaultRequestHeaders.UserAgent.ParseAdd("PointexProvisioningQr");

            var bytes = await client.GetByteArrayAsync(QrCodeUrl);

            using var stream = new MemoryStream(bytes);
            using var downloaded = new Bitmap(stream);
            _pictureBox.Image = new Bitmap(downloaded);
            _statusLabel.Visible = false;
        }
        catch (Exception ex)
        {
            _statusLabel.Text = "Échec du téléchargement : " + ex.Message;
        }
    }
}
