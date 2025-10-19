package ir.zemestoon.silentsound;

public class Encoder {
    public static String encodeString(String input) {
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String map = "zuPTUVWXNOopixyEAeRntlmwSghBDaJFYvjkKCfQbcdrsLMGHIqZ";

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char char_i = input.charAt(i);
            if (i % 2 == 0) {
                output.append(char_i);
                continue;
            }
            boolean find = false;
            for (int j = 0; j < alphabet.length(); j++) {
                char letter = alphabet.charAt(j);
                if (char_i == letter) {
                    output.append(map.charAt(j));
                    find = true;
                    break;
                }
            }
            if (!find) output.append(char_i);
        }
        return output.toString();
    }

    public static String decodeString(String input) {
        String alphabet = "zuPTUVWXNOopixyEAeRntlmwSghBDaJFYvjkKCfQbcdrsLMGHIqZ";
        String map = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char char_i = input.charAt(i);
            if (i % 2 == 0) {
                output.append(char_i);
                continue;
            }
            boolean find = false;
            for (int j = 0; j < alphabet.length(); j++) {
                char letter = alphabet.charAt(j);
                if (char_i == letter) {
                    output.append(map.charAt(j));
                    find = true;
                    break;
                }
            }
            if (!find) output.append(char_i);
        }
        return output.toString();
    }

}
