package play.autocomplete;

import java.util.ArrayList;
import java.util.List;

public class Trie {
	Item root = new Item((char) 0);
	static int n = 0;
	
	static public class Item {
		private static final int START = ' ';
		private boolean endOfWord = false;
		char c;
		Item[] items;
		

		public Item(char c) {
			this.c = c;
		}

		public Item add(char c) {
			Item next = get(c);
			if (next == null) {
				next = new Item(c);
				set(c, next);
			}
			return next;
		}

		private Item set(char c, Item next) {
			int cc = translate(c);
			if ( items == null)
				items = new Item[128 - START];
			return items[cc] = next;
		}

		private Item get(char c) {
			if ( items == null)
				return null;
			
			int cc = translate(c);
			return items[cc];
		}

		public List<String> completions(String prefix) {
			return suffixes(new StringBuilder(prefix), new ArrayList<String>());
		}

		private List<String> suffixes(StringBuilder sb, ArrayList<String> result) {
			if (endOfWord)
				result.add(sb.toString());

			for (int i = 0; items != null && i < items.length; i++) {
				if (items[i] != null) {
					int mark = sb.length();
					sb.append(items[i].c);
					items[i].suffixes(sb,result);
					sb.setLength(mark);
				}
			}
			return result;
		}
		
		@Override
		public String toString() {
			return "Item [endOfWord=" + endOfWord + ", c=" + c + "]";
		}

		private int translate(char c) {
			if (c >= 128)
				return '*';

			if ( Character.isUpperCase(c))
				c = Character.toLowerCase(c);
			return c-START;
		}
		
	}

	public Item findPrefix(String s) {
		if (s.length() == 0)
			return null;

		Item rover = root;
		s = s.trim();
		for (int i = 0; i < s.length() && rover != null; i++) {
			char c = s.charAt(i);
			rover = rover.get(c);
		}
		return rover;
	}

	public void add(String word) {
		Item rover = root;
		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			rover = rover.add(c);
		}
		rover.endOfWord = true;
	}
}
