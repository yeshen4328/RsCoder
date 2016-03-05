import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class Io {

	public Io() {
		// TODO Auto-generated constructor stub
	}
	public static int[] readBitAndStranToByte(String path)
	{
		FileReader fr = null;
		BufferedReader reader = null;;
		int size = 0;
		byte[] dataInBit = null;
		int[] realData = null;
		try {
				fr = new FileReader(path);
				reader = new BufferedReader(fr);
				String str = reader.readLine();
				String[] bit = str.split(" ");
				dataInBit = new byte[bit.length];
				for(int i = 0; i < bit.length; i++)
					dataInBit[i] = (byte) (bit[i].equals("1") ? 1: 0);
				realData = new int[bit.length / 8];
				byte[] temp = new byte[8];
				
				for(int i = 0; i < realData.length; i++)
				{
					System.arraycopy(dataInBit, i * 8, temp, 0, 8);
					realData[i] = DoubleRadix2Byte(temp);
				}
				fr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return realData;
	}
	public static int DoubleRadix2Byte(byte[] d)
	{
		int out = 0;
		for(int i = 0; i < 8; i++)		
			out += d[i] * Math.pow(2, 7 - i);	
		return out;
	}
}
