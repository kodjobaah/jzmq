package com.watamidoing.camera;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import nayuki.arithcode.ArithmeticCompress;
import nayuki.arithcode.ArithmeticDecompress;
import nayuki.arithcode.BitInputStream;
import nayuki.arithcode.BitOutputStream;
import nayuki.arithcode.FrequencyTable;
import nayuki.arithcode.SimpleFrequencyTable;


public class ArithmeticCodeCompression {


		public byte[] compress(byte[] b) throws IOException {
			InputStream in = new ByteArrayInputStream(b);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			BitOutputStream bitOut = new BitOutputStream(out);

			FrequencyTable freq = getFrequencies(b);
			ArithmeticCompress.writeFrequencies(bitOut, freq);
			ArithmeticCompress.compress(freq, in, bitOut);
			bitOut.close();
			return out.toByteArray();
		}


		public byte[] decompress(byte[] b) throws IOException {
			InputStream in = new ByteArrayInputStream(b);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			BitInputStream bitIn = new BitInputStream(in);

			FrequencyTable freq = ArithmeticDecompress.readFrequencies(bitIn);
			ArithmeticDecompress.decompress(freq, bitIn, out);
			return out.toByteArray();
		}


		private static FrequencyTable getFrequencies(byte[] b) {
			FrequencyTable freq = new SimpleFrequencyTable(new int[257]);
			for (byte x : b)
				freq.increment(x & 0xFF);
			freq.increment(256);  // EOF symbol gets a frequency of 1
			return freq;
		}

}
