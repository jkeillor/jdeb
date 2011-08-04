package org.vafer.jdeb.conffiles;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.vafer.jdeb.utils.Utils;
import org.vafer.jdeb.utils.VariableResolver;

public class Conffiles {

    private final FileInputStream fileInputStream;
    private final VariableResolver resolver;
    private static String openToken = "[[";
    private static String closeToken = "]]";
    private List<String> lines = new ArrayList<String>();

    public Conffiles(FileInputStream pFileInputStream, VariableResolver pResolver) throws IOException, ParseException {
        fileInputStream = pFileInputStream;
        resolver = pResolver;
        parse();
    }

    public static void setOpenToken( final String pToken ) {
        openToken = pToken;
    }

    public static void setCloseToken( final String pToken ) {
        closeToken = pToken;
    }

    private void parse() throws IOException, ParseException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(fileInputStream));
            String line;
            int lineNumber = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.length() == 0) {
                    throw new ParseException("Empty line", lineNumber);
                }
                lines.add(Utils.replaceVariables(resolver, line, openToken, closeToken));
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append('\n');
        }
        return builder.toString();
    }

}
