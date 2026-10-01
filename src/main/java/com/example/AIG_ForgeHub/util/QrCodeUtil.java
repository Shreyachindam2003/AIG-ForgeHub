package com.example.AIG_ForgeHub.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Component
public class QrCodeUtil {

    public String generateQrCode(String text) {

        try {

            QRCodeWriter qrCodeWriter =
                    new QRCodeWriter();

            BitMatrix bitMatrix =
                    qrCodeWriter.encode(
                            text,
                            BarcodeFormat.QR_CODE,
                            300,
                            300
                    );

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    "PNG",
                    outputStream
            );

            String base64 =
                    Base64.getEncoder()
                            .encodeToString(
                                    outputStream.toByteArray()
                            );

            return "data:image/png;base64," + base64;

        } catch (WriterException | java.io.IOException e) {

            throw new RuntimeException(
                    "Unable to generate QR code",
                    e
            );
        }
    }
}