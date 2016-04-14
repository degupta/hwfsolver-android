package com.devansh.hwfsolver.com.devansh.dawg;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;

public class DawgArray {
	byte[] dawgArray = null;

	public DawgArray(InputStream in) {
		createDawgFromFileForArrayLookup(in);
	}

	public void createDawgFromFileForArrayLookup(InputStream in) {
		try {
			int val = 0, i = 0;
			// The first four bytes will specify how many bytes are there in the
			// file (minus the first four bytes)
			int size = (in.read() << 24) | (in.read() << 16) | (in.read() << 8)
					| in.read();
			// Create an array of that size
			dawgArray = new byte[size];
			// Read in array byte by byte
			while ((val = in.read()) != -1) {
				dawgArray[i++] = (byte) (val);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int unsignedToBytes(byte b) {
		return b & 0xFF;
	}

	/**
	 * Checks to see if a word exists
	 */
	public boolean wordExists(String word) {
		int length = word.length();
		if (length == 0)
			return false;

		char currentChar = (char) 0;
		int currentNode = 0;
		int currentIndex = 0;
		int childPos = 0;
		int childArrPos = 0;
		int pos = 0;
		while (true) {
			pos = currentNode * 4;
			// Get the index of the list of children
			childPos = (unsignedToBytes(dawgArray[pos]) << 16)
					| (unsignedToBytes(dawgArray[pos + 1]) << 8)
					| (unsignedToBytes(dawgArray[pos + 2]));
			childPos = childPos >> 2;
			// If the index was 0 we have no children and we haven't reached the
			// end of the word => word doesn't exist
			if (childPos == 0)
				return false;
			// Go through the list of children and find the one with the
			// character we are looking for
			while (true) {
				childArrPos = childPos * 4;
				currentChar = (char) (dawgArray[childArrPos + 3]);
				if (currentChar == word.charAt(currentIndex)) {
					// We found the character move on
					currentIndex++;
					break;
				}
				// If we have reached end of list but still haven't found the
				// character this word doesn't exist
				if ((dawgArray[childArrPos + 2] & 2) > 0)
					return false; // Is End Of List
				childPos++;
			}

			// Have we reached the last letter in the word. If yes check
			// whether the current node has its end of word flag set!
			if (currentIndex == length)
				return (dawgArray[childArrPos + 2] & 1) > 0; // Is Final Node

			// Recurse on child
			currentNode = childPos;
		}
	}

	/**
	 * Checks whether the list of children has 'letter' and whether it is at an
	 * end-of-word
	 */
	public boolean isChildEndOFWord(int currentNode, char letter) {
		int pos = currentNode * 4;
		int childPos = (unsignedToBytes(dawgArray[pos]) << 16)
				| (unsignedToBytes(dawgArray[pos + 1]) << 8)
				| (unsignedToBytes(dawgArray[pos + 2]));
		childPos = childPos >> 2;
		// This thing has no children
		if (childPos == 0)
			return false;
		int childArrPos = 0;
		while (true) {
			childArrPos = childPos * 4;
			// Found the letter, check whether it is end of word
			if ((char) (dawgArray[childArrPos + 3]) == letter)
				return (dawgArray[childArrPos + 2] & 1) > 0;
			// We haven't found the letter but are at the end of the list of
			// children, break out.
			if ((dawgArray[childArrPos + 2] & 2) > 0)
				break;
			childPos++;
		}

		// Child not found return false
		return false;
	}

	/**
	 * Checks whether the node is end of a word
	 */
	public boolean isEndOFWord(int currentNode) {
		return (dawgArray[currentNode * 4 + 2] & 1) > 0;
	}

	/**
	 * Returns a hashmap with the child's letter as the key and the position its
	 * index as the value
	 */
	public HashMap<Character, Integer> getChildren(int currentNode) {
		HashMap<Character, Integer> children = new HashMap<Character, Integer>();
		int pos = currentNode * 4;
		int childPos = (unsignedToBytes(dawgArray[pos]) << 16)
				| (unsignedToBytes(dawgArray[pos + 1]) << 8)
				| (unsignedToBytes(dawgArray[pos + 2]));
		childPos = childPos >> 2;
		// It has no children, return empty hashmap
		if (childPos == 0)
			return children;
		int childArrPos = 0;
		// Go through list of children
		while (true) {
			childArrPos = childPos * 4;
			// Put the child and its index in the hashmap
			children.put((char) (dawgArray[childArrPos + 3]), childPos);
			// End of list of children
			if ((dawgArray[childArrPos + 2] & 2) > 0)
				break;
			childPos++;
		}

		return children;
	}

	/**
	 * Return all words in the DAWG
	 */
	public HashSet<String> getAllWords() {
		HashSet<String> words = new HashSet<String>(170000);
		getAllWords(words, new StringBuilder(), 0);
		return words;
	}

	private void getAllWords(HashSet<String> words, StringBuilder word,
			int currentNode) {
		// Is Final Node
		if (isEndOFWord(currentNode)) {
			words.add(new String(word));
		}

		int pos = currentNode * 4;
		int childPos = (unsignedToBytes(dawgArray[pos]) << 16)
				| (unsignedToBytes(dawgArray[pos + 1]) << 8)
				| (unsignedToBytes(dawgArray[pos + 2]));
		childPos = childPos >> 2;
		if (childPos == 0) {
			return;
		}
		int childArrPos = 0;
		// Go through list of children
		while (true) {
			childArrPos = childPos * 4;
			word.append((char) (dawgArray[childArrPos + 3]));
			getAllWords(words, word, childPos);
			word.deleteCharAt(word.length() - 1);
			// End of list of children
			if ((dawgArray[childArrPos + 2] & 2) > 0) {
				break;
			}
			childPos++;
		}
	}
}
