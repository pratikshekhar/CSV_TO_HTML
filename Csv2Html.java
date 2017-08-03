/*
 * @author Colin S. Gordon (csg63@drexel.edu)
 *
 * Code to convert a CSV file to an HTML table.
 *
 * The default CSV format is agnostic about things like escaping characters of the input.  This
 * implementation:
 *  - Escapes various HTML entities as appropriate ( < > & ; \ " )
 *  - Uses backslash escapes to permit commas inside cells
 *  - Inside double-quotes, commas are interpreted as normal characters, while other special
 *  characters may be escaped with backslashes
 *
 * It takes two command line arguments.  First, a mandatory input filename.  Second, an *optional*
 * output filename.  If no output file is specified, a .html file with the same basename as the
 * input file is used (e.g., foo.csv is converted into a table in foo.html).
 *
 * You can use your favorite browser to view the input files (most have an "Open File" option,
 * which directs the browser to a local file using the file:// protocol).
 *
 */
import java.util.List;
import java.util.LinkedList;
import java.io.BufferedReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.PrintWriter;
 
class Csv2Html {

    public static List<List<String>> parseInput(BufferedReader input) throws IOException {
        String line = null;
        List<List<String>> out = new LinkedList<>();
        while ((line = input.readLine()) != null) {
            List<String> parsed = new LinkedList<>();

            int i = 0;
            boolean in_quotes = false;
            StringBuilder sb = new StringBuilder();
            while (i < line.length()) {
                char curr = line.charAt(i);
                switch (curr) {
                    case '"':
                        if (in_quotes) {
                            in_quotes = false;
                        } else {
                            in_quotes = true;
                        }
                        i++;
                        break;
                    case '\\':
                        char c = line.charAt(i+1);
                        if (c == ',' || c == '"' || c == '\\') {
                            sb.append(c);
                            i+=2;
                        } else {
                            // not an escapable character
                            sb.append('\\');
                            i++;
                        }
                        break;
                    case ',':
                        if (in_quotes) {
                            sb.append(',');
                        } else {
                            parsed.add(sb.toString());
                            sb = new StringBuilder();
                        }
                        i++;
                        break;
                    default:
                        sb.append(curr);
                        i++;
                }
            }
            if (sb.length() > 0) {
                parsed.add(sb.toString());
            }
            sb = null;

            // finish single line
            out.add(parsed);
        }
        return out;
    }

    public static String escape(String s) {
        return s.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\'", "&apos;")
                .replaceAll("&", "&amp;")
                .replaceAll("\"", "&quot;");
    }

    public static void emitHTML(List<List<String>> cells, PrintWriter p) {
        p.println("<html>");
        p.println("<body>");
        p.println("<table border=\"1\">");
        for (List<String> row : cells) {
            p.println("  <tr>");
            for (String value : row) {
                p.print("    <td>");
                p.print(escape(value));
                p.println("</td>");
            }
            p.println("  </tr>");
        }
        p.println("</table>");
        p.println("</body>");
        p.println("</html>");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Missing filename.");
            System.exit(-1);
        }
        Path file = null;
        try {
            file = FileSystems.getDefault().getPath(args[0]);
        } catch(InvalidPathException e) {
            System.err.println("Invalid file: "+args[0]);
            System.exit(-2);
        }

        BufferedReader input = null;
        try {
            input = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("IO Exception opening "+file+"\n"+e.toString());
            e.printStackTrace();
            System.exit(-3);
        } catch (SecurityException e) {
            System.err.println("SecurityException (probably not good):");
            System.err.println(e.toString());
            e.printStackTrace();
            System.exit(-4);
        }


        List<List<String>> cells = null;
        try {
            cells = parseInput(input);
            input.close();
        } catch (IOException e) {
            System.err.println("IO Exception while parsing: "+e);
            e.printStackTrace();
            System.exit(-5);
        }

        String output = args[0].replace("csv", "html");
        if (args.length > 1) {
            output = args[1];
        }

        Path out = null;
        PrintWriter w = null;
        try {
            out = FileSystems.getDefault().getPath(output);
            w = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8, 
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.APPEND,
                                        StandardOpenOption.WRITE));
        } catch (IOException e) {
            System.err.println("Problem opening output file ["+out+"]:");
            System.err.println(e.toString());
            e.printStackTrace();
        }
        
        emitHTML(cells, w);

        w.close();
        
    }
    
}
 
