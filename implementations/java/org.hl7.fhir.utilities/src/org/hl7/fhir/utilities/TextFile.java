/*
Copyright (c) 2011-2013, HL7, Inc
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this 
   list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
   this list of conditions and the following disclaimer in the documentation 
   and/or other materials provided with the distribution.
 * Neither the name of HL7 nor the names of its contributors may be used to 
   endorse or promote products derived from this software without specific 
   prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

*/
package org.hl7.fhir.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class TextFile {

	public static List<String> readAllLines(String path) throws IOException
	{
		List<String> result = new ArrayList<String>();
		
		File file = new CSFile(path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
		
		while( reader.ready() )
			result.add(reader.readLine());
		
		reader.close();
		return result;
	}
	
	public static void writeAllLines(String path, List<String> lines) throws IOException
	{
		File file = new CSFile(path);
		FileOutputStream s = new FileOutputStream(file);
		OutputStreamWriter sw = new OutputStreamWriter(s, "UTF-8");
		
		for( String line : lines )
			sw.write(line + "\r\n");
		
		sw.flush();
		s.close();
	}
	
	
    public static void stringToFile(String content, String path) throws Exception {
		File file = new CSFile(path);
		OutputStreamWriter sw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		sw.write('\ufeff');
		sw.write(content);
		sw.flush();
		sw.close();
	}

  public static String fileToString(String src) throws Exception {
    return streamToString(new FileInputStream(new CSFile(src)));
	}

  public static String streamToString(InputStream input) throws Exception {
    InputStreamReader sr = new InputStreamReader(input, "UTF-8");
    StringBuilder b = new StringBuilder();
    while (sr.ready()) {
      char c = (char) sr.read();
      b.append(c);
    }
    sr.close();
    return b.toString();
  }
}
