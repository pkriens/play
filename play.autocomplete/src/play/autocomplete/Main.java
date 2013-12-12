package play.autocomplete;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import play.autocomplete.Trie.Item;
import aQute.lib.collections.LineCollection;
import aQute.lib.io.IO;

public class Main {

	public static void main(String args[]) throws IOException {
		final Trie ac = new Trie();
		LineCollection lc = new LineCollection(IO.reader(new FileInputStream(
				"data.txt")));
		try {
			while (lc.hasNext()) {
				String line = lc.next().trim();
				if (line.isEmpty())
					break;

				ac.add(line);
			}
		} finally {
			lc.close();
		}

		ConsoleReader r = new ConsoleReader();
		r.addCompleter(new Completer() {

			@Override
			public int complete(String buffer, int cursor,
					List<CharSequence> candidates) {
				Item prefix = ac.findPrefix(buffer);
				if (prefix != null) {
					List<String> completions = prefix.completions("");
					candidates.addAll(completions);
				}
				return buffer.length();
			}
		});
		while (true) {
			System.out.print("> ");
			System.out.println(r.readLine());
		}

	}
}
