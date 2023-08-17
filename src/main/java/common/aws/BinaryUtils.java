package common.aws;

import com.ksc.KscClientException;
import com.ksc.internal.SdkDigestInputStream;
import com.ksc.util.Base16Lower;
import com.ksc.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public class BinaryUtils {

    public BinaryUtils() {
    }

    public static String toHex(byte[] data) {
        return Base16Lower.encodeAsString(data);
    }

    public static byte[] fromHex(String hexData) {
        return Base16Lower.decode(hexData);
    }

    public static String toBase64(byte[] data) {
        return Base64.encodeAsString(data);
    }

    public static byte[] fromBase64(String b64Data) {
        return b64Data == null ? null : Base64.decode(b64Data);
    }

    public static ByteArrayInputStream toStream(ByteBuffer byteBuffer) {
        return byteBuffer == null ? new ByteArrayInputStream(new byte[0]) : new ByteArrayInputStream(copyBytesFrom(byteBuffer));
    }

    public static byte[] copyAllBytesFrom(ByteBuffer bb) {
        if (bb == null) {
            return null;
        } else if (bb.hasArray()) {
            return Arrays.copyOfRange(bb.array(), bb.arrayOffset(), bb.arrayOffset() + bb.limit());
        } else {
            ByteBuffer copy = bb.asReadOnlyBuffer();
            copy.rewind();
            byte[] dst = new byte[copy.remaining()];
            copy.get(dst);
            return dst;
        }
    }

    public static byte[] copyBytesFrom(ByteBuffer bb) {
        if (bb == null) {
            return null;
        } else if (bb.hasArray()) {
            return Arrays.copyOfRange(bb.array(), bb.arrayOffset() + bb.position(), bb.arrayOffset() + bb.limit());
        } else {
            byte[] dst = new byte[bb.remaining()];
            bb.asReadOnlyBuffer().get(dst);
            return dst;
        }
    }


    public static byte[] hash(InputStream input) throws KscClientException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            DigestInputStream digestInputStream = new SdkDigestInputStream(input, md);
            byte[] buffer = new byte[1024];

            while (digestInputStream.read(buffer) > -1) {
            }

            return digestInputStream.getMessageDigest().digest();
        } catch (Exception var5) {
            throw new KscClientException("Unable to compute hash while signing request: " + var5.getMessage(), var5);
        }
    }


    public static byte[] hash(String text) throws KscClientException {
        return doHash(text);
    }

    public static byte[] doHash(String text) throws KscClientException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes(com.ksc.util.StringUtils.UTF8));
            return md.digest();
        } catch (Exception var2) {
            throw new KscClientException("Unable to compute hash while signing request: " + var2.getMessage(), var2);
        }
    }
}
