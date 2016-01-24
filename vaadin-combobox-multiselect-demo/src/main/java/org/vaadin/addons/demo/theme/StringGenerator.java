package org.vaadin.addons.demo.theme;

public class StringGenerator {

    static String[] strings = new String[] { "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "quid", "securi", "etiam", "tamquam", "eu", "fugiat", "nulla", "pariatur" };
    int stringCount = -1;

    String nextString(boolean capitalize) {
        if (++stringCount >= strings.length) {
            stringCount = 0;
        }
        return capitalize ? strings[stringCount].substring(0, 1)
                                                .toUpperCase()
                + strings[stringCount].substring(1) : strings[stringCount];
    }

}
