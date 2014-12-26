package jp.co.pokemon.cardboardsumo.sumo;

/**
 * Created by psyark on 2014/12/20.
 */
public class DebugUtil {
    public static String hexDump(byte[] bytes) {
        String dump = "";
        int max = Math.min(bytes.length, 32);
        for (int i = 0; i < max; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            dump += hex + " ";
        }
        return dump;
    }
}
