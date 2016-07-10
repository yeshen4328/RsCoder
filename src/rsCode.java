import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.security.sasl.RealmCallback;

public class rsCode
{  
	private static final int BITS_ALL = 6000;
    private static final int MM = 4;  
    private static final int NN = 15;  
    private static final int KK = 1;  
    private static final int TT = (NN - KK) / 2; 
    private int[] pp = {1,1,0,0,1}  ;//  N255K127{1,0,1,1,1,0,0,0,1}    N15K11M4{1,1,0,0,1}
    //private int[] pp = {1,1,0,0,1};
    private int[] alphaTo = new int[NN+1];  
    private int[] indexOf = new int[NN+1];
    private int[] gg = new int[NN-KK+1];
    public int[] recd = new int[NN];  
    public int[] data = new int[KK];  //信息码位
    public int[] bb = new int[NN-KK];  //保存最后的校验位
      
    /** 
     * 构造函数RSCode() 
     * 初始化工作，生成GF空间和对应的生成多项式 
     */  
    public rsCode()
    {  
        generateGF();  
        generatePolynomial();  
    }  
      
    /** 
     * 函数generateGF() 
     * 生成GF(2^MM)空间 
     */  
    public void generateGF() 
    {  
        int i, mask;  
        mask = 1;  
        alphaTo[MM] = 0;  
        for(i=0; i<MM; i++)
        {  
            alphaTo[i] = mask;  
            indexOf[alphaTo[i]] = i;  
            if(pp[i] != 0)
            {  
                alphaTo[MM] ^= mask;  
            }  
            mask <<= 1;  
        }  
          
        indexOf[alphaTo[MM]] = MM;  
        mask >>= 1;  
          
        for(i=MM+1; i<NN; i++)
        {  
            if(alphaTo[i-1] >= mask)            
                alphaTo[i] = alphaTo[MM] ^ ((alphaTo[i-1]^mask)<<1);           
            else            
                alphaTo[i] = alphaTo[i-1]<<1;              
            indexOf[alphaTo[i]] = i;  
        }  
          
        indexOf[0] = -1;  
        //输出GF空间  
/*      System.out.println("GF空间:"); 
        for(i=0; i<=NN; i++){ 
            System.out.println(i + "   " + alphaTo[i] + "   " + indexOf[i]); 
        }*/  
    }//GenerateGF  
      
    /** 
     * 函数generatePolynomial() 
     * 产生相应的生成多项式的各项系数 
     */  
    public void generatePolynomial() 
    {  
        int i, j;  
        gg[0] = 2;  
        gg[1] = 1;  
        for(i=2; i<=NN-KK; i++) 
        {  
            gg[i] = 1;  
            for(j=i-1; j>0; j--)
            {  
                if(gg[j] != 0)  
                    gg[j] = gg[j-1] ^ alphaTo[(indexOf[gg[j]]+i) % NN];  
                else  
                    gg[j] = gg[j-1];  
            }  
            gg[0] = alphaTo[(indexOf[gg[0]]+i) % NN];  
        }  
          
        //转换其到  
        for(i=0; i<=NN-KK; i++) 
        {  
            gg[i] = indexOf[gg[i]];  
        }  
          
        //输出生成多项式的各项系数  
        System.out.println("生成多项式系数:");  
        for(i=0; i<=NN-KK; i++) 
        {  
            System.out.println(gg[i]);  
        }  
    }  
  
    /** 
     * 函数rsEncode() 
     * RS编码 
     */  
    public void rsEncode()
 {  
        int i, j;  
        int feedback;  
        for(i=0; i<NN-KK; i++)  
           bb[i] = 0;  
          
        for(i = KK-1; i >= 0; i --) 
        {  
            //逐步的将下一步要减的，存入bb(i)  
            feedback = indexOf[data[i] ^ bb[NN-KK-1]];  
            if(feedback != -1) {  
                for(j=NN-KK-1; j>0; j--) {  
                    if(gg[j] != -1)  
                        bb[j] = bb[j-1] ^ alphaTo[(gg[j]+feedback)%NN];  
                    else  
                        bb[j] = bb[j-1];  
                }  
                bb[0] = alphaTo[(gg[0]+feedback)%NN];  
            }else {  
                for(j=NN-KK-1; j>0; j--) {  
                    bb[j] = bb[j-1];  
                }  
                bb[0] = 0;  
            }  
        }  
        //输出编码结果  
        System.out.println("编码结果:");  
        for(i=0; i<NN-KK; i++) {  
            System.out.println(bb[i]);  
        }  
    }  
      
    public void putData(byte[] data)
    {
    	int[] dataInInt = new int[data.length];
    	for(int i = 0; i < data.length; i++)
    		dataInInt[i] = (int)data[i];
    	this.data = dataInInt;
    }
    public void putData(int[] data)
    {
    	this.data = data.clone();
    }
    public int[] getCode()
    {
    	return bb.clone();
    }
    public void putToDecode(int[] data)
    {
    	recd = data;
    }
    /** 
     * 函数rsDecode() 
     * RS解码 
     */  
    public void rsDecode()
    {  
        int i, j, u, q;  
        int[][] elp = new int[NN-KK+2][NN-KK];  
        int[] d = new int[NN-KK+2];  
        int[] l = new int[NN-KK+2];  
        int[] u_lu = new int[NN-KK+2];  
        int[] s = new int[NN-KK+1];  
          
        int count = 0;  
        int syn_error = 0;  
        int[] root = new int[TT];  
        int[] loc = new int[TT];  
        int[] z = new int[TT+1];  
        int[] err = new int[NN];  
        int[] reg = new int[TT+1];  
          
        //转换成GF空间  
        for(i=0; i<NN; i++)
        {  
            if(recd[i] == -1)  
                recd[i] = 0;  
            else  
                recd[i] = indexOf[recd[i]];  
        }
          
        //求伴随多项式  
        for(i=1; i<=NN-KK; i++)
        {  
            s[i] = 0;  
            for(j=0; j<NN; j++)
            {  
                if(recd[j] != -1)  
                    s[i] ^= alphaTo[(recd[j]+i*j)%NN];  
            }  
            if(s[i] != 0)  
                syn_error = 1;  
            s[i] = indexOf[s[i]];  
        }  
        System.out.println("syn_error=" + syn_error);  
          
        //如果有错，则进行纠正  
        if(syn_error == 1) 
        {  
            //BM迭代求错误多项式的系数  
            d[0] = 0;  
            d[1] = s[1];  
            elp[0][0] = 0;  
            elp[1][0] = 1;  
            for(i=1; i<NN-KK; i++) 
            {  
                elp[0][i] = -1;  
                elp[1][i] = 0;  
            }  
            l[0] = 0;  
            l[1] = 0;  
            u_lu[0] = -1;  
            u_lu[1] = 0;  
            u = 0;  
            do {  
                u++;  
                if(d[u] == -1)
                {  
                    l[u+1] = l[u];  
                    for(i=0; i<=l[u]; i++)
                    {  
                        elp[u+1][i] = elp[u][i];  
                        elp[u][i] = indexOf[elp[u][i]];  
                    }  
                }
                else 
                {  
                    q = u-1;  
                    while((d[q]==-1) && (q>0))                  
                        q--;  
                      
                    if(q > 0) 
                    {  
                        j = q;  
                        do {  
	                            j--;  
	                            if((d[j] != -1) && (u_lu[q] < u_lu[j])) 
	                            {  
	                                q = j;  
	                            }  
                        }while(j > 0);  
                    }  
                      
                    if(l[u] > l[q] + u - q)
                    {  
                        l[u+1] = l[u];  
                    }
                    else
                    {  
                        l[u+1] = l[q] + u -q;  
                    }  
                      
                    for(i=0; i<NN-KK; i++) 
                    {  
                        elp[u+1][i] = 0;  
                    }  
                    for(i=0; i<=l[q]; i++)
                    {  
                        if(elp[q][i] != -1)  
                            elp[u+1][i+u-q] = alphaTo[(d[u]+NN-d[q]+elp[q][i])%NN];  
                    }  
                      
                    for(i=0; i<=l[u]; i++)
                    {  
                        elp[u+1][i] ^= elp[u][i];  
                        elp[u][i] = indexOf[elp[u][i]];  
                    }  
                }  
                u_lu[u+1] = u-l[u+1];  
                  
                if(u < NN-KK)
                {  
                    if(s[u+1] != -1) 
                    {  
                        d[u+1] = alphaTo[s[u+1]];  
                    }
                    else
                    {  
                        d[u+1] = 0;  
                    }  
                      
                    for(i=1; i<=l[u+1]; i++)
                    {  
                        if((s[u+1-i] != -1) && (elp[u+1][i] != 0))
                        {  
                            d[u+1] ^= alphaTo[(s[u+1-i]+indexOf[elp[u+1][i]])%NN];  
                        }  
                    }  
                    d[u+1] = indexOf[d[u+1]];  
                }  
            }while((u<NN-KK) && (l[u+1]<=TT));  
            u++;  
            System.out.println("错误数目:" + l[u]);  
              
            //求错误位置，以及改正错误  
            if(l[u] <= TT)
            {  
                for(i=0; i<= l[u]; i++)
                {  
                    elp[u][i] = indexOf[elp[u][i]];  
                }  
                //求错误位置多项式的根  
                for(i=1; i<= l[u]; i++) 
                {  
                    reg[i] = elp[u][i];  
                }  
                count = 0;  
                for(i=1; i<=NN; i++) 
                {  
                    q = 1;  
                    for(j=1; j<=l[u]; j++)
                    {  
                        if(reg[j]!=-1)
                        {  
                            reg[j] = (reg[j] + j)%NN;  
                            q ^= alphaTo[reg[j]];  
                        }  
                    }  
                      
                    if(q==0)
                    {  
                        root[count] = i;  
                        loc[count] = NN-i;  
                        System.out.println("错误位置:" + loc[count]);  
                        count++;                          
                    }  
                }  
                  
                //  
                if(count == l[u]) 
                {  
                    for(i=1; i<=l[u]; i++) 
                    {  
                        if((s[i]!=-1) && elp[u][i]!=-1) 
                        {  
                            z[i] = alphaTo[s[i]] ^ alphaTo[elp[u][i]];  
                        }
                        else if((s[i]!=-1) && (elp[u][i]==-1)) 
                        {  
                            z[i] = alphaTo[s[i]];  
                        }
                        else if((s[i]==-1) && (elp[u][i]!=-1)) 
                        {  
                            z[i] = alphaTo[elp[u][i]] ;  
                        }
                        else
                        {  
                            z[i] = 0;  
                        }  
                          
                        for(j=1; j<i; j++)
                        {  
                            if((s[j]!=-1) && (elp[u][i-j]!=-1))
                            {  
                                z[i] ^= alphaTo[(elp[u][i-j] + s[j])%NN];  
                            }  
                        }                           
                        z[i] = indexOf[z[i]];  
                    }  
                      
                    //计算错误图样  
                    for(i=0; i<NN; i++) 
                    {  
                        err[i] = 0;  
                        if(recd[i] != -1)  
                            recd[i] = alphaTo[recd[i]];  
                        else  
                            recd[i] = 0;  
                    }  
                    for(i=0; i<l[u]; i++) 
                    {  
                        err[loc[i]] = 1;  
                        for(j=1; j<=l[u]; j++) 
                        {  
                            if(z[j] != -1)  
                                err[loc[i]] ^= alphaTo[(z[j]+j*root[i])%NN];  
                        }  
                          
                        if(err[loc[i]] != 0) 
                        {  
                            err[loc[i]] = indexOf[err[loc[i]]];  
                            q = 0;  
                            for(j=0; j<l[u]; j++)
                            {  
                                if(j!=i)  
                                    q += indexOf[1^alphaTo[(loc[j]+root[i])%NN]];  
                            }  
                            q = q%NN;  
                            err[loc[i]] = alphaTo[(err[loc[i]]-q+NN)%NN];  
                            recd[loc[i]] ^= err[loc[i]];  
                        }  
                    }  
                }
                else 
                {  
                    //错误太多，无法改正  
                    for(i=0; i<NN; i++) 
                    {  
                        if(recd[i] != -1)  
                            recd[i] = alphaTo[recd[i]];  
                        else  
                            recd[i] = 0;  
                    }  
                }  
            }
            else
            {  
                //错误太多，无法改正  
                for(i=0; i<NN; i++)
                {  
                    if(recd[i] != -1)  
                        recd[i] = alphaTo[recd[i]];  
                    else  
                        recd[i] = 0;  
                }  
            }  
        }
        else 
        {  
            for(i=0; i<NN; i++) 
            {  
                if(recd[i] != -1)  
                    recd[i] = alphaTo[recd[i]];  
                else  
                    recd[i] = 0;  
            }  
        }  
    }  
    /** 
     * @param args 
     */  
    public int[] getCaledReceive()
    {
    	return recd.clone();
    }
    public static void main(String[] args)
    {  
        // TODO Auto-generated method stub  
	    if(true)
	    {
	        rsCode rs = new rsCode();        
	        //************输入要编码的数据 **************编码*****************
	        //二重纠错，每16个码字一组，其中包括15码字用来做rs编码，还有一个码字用来判断信息码是否正确。
	        FileInputStream fis = null;
			byte[] data = null;
			int[] dataNew = null;
			int setNum = BITS_ALL / (NN + 1) / MM / 5;
			try {
					fis = new FileInputStream("./surface.txt");
					int size = fis.available();
					size = setNum * (5 - 1) * KK * MM / 8;
					data = new byte[size];
					fis.read(data);
					fis.close(); 
					dataNew = new int[2 * size];
					for(int i = 0, j = 0; i < size; i++, j+=2)
					{
						dataNew[j] = (int)(data[i] >> 4 & 0x0f);
						dataNew[j + 1] = (int)(data[i]  & 0x0f);
					}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			int blockNum = setNum * 5;
			int[][] dataToEncode = new int[blockNum][KK];
			//int[] code = new int[(NN) * blockNum];
			int[] code = new int[BITS_ALL / MM];
			for(int i = 0, p = 0; i < blockNum; i++)
			{
				if((i+1)%5 == 0)
				{
					int xorArr = 0;
					for(int j = 0; j < 4; j++)
						xorArr ^= dataToEncode[i - j - 1][0];
					int[] tmp = new int[1];
					tmp[0] = xorArr;
					rs.putData(tmp);
					rs.rsEncode();
					int[] res = rs.getCode();
					System.arraycopy(res, 0, code, i * (NN + 1), NN - KK);
					System.arraycopy(new int[]{xorArr}, 0, code, i*(NN + 1) + NN - KK, KK);
					System.arraycopy(new int[]{xorArr}, 0, code, i * (NN + 1) + NN, KK);
					continue;
				}
				System.arraycopy(dataNew, p++ * KK, dataToEncode[i], 0, KK);
				rs.putData(dataToEncode[i]);
				rs.rsEncode();
				int[] result = rs.getCode();
				System.arraycopy(result, 0, code, i * (NN + 1), NN - KK);//校验码
				System.arraycopy(dataToEncode[i], 0, code, i * (NN + 1) + NN - KK, KK);//信息码
				int xor = getXor(dataToEncode[i]);
				System.arraycopy(new int[]{xor}, 0, code, i * (NN + 1) + NN, KK);//异或
			}
			
/*			int[] writeToFile = new int[6400/MM];
//************************************************分配**************************************************V2.1
			for(int i = 0; i < 6400/MM/(NN + 1); i++)
			{
				 System.arraycopy(code, i * NN, writeToFile, i * (NN + 1), NN);
				 writeToFile[i * NN] = code[i * NN - 1];
			}*/
			 
			try {
					FileWriter fw = new FileWriter("./msg_surface_double_15_1_6k.txt");
					for(int i = 0;i < code.length; i++)
					{
						byte[] b = byteArr2DoubleRadix(code[i]);
						for(int j = 0; j < MM; j++)	
							fw.write(Byte.toString(b[j]) + " ");
					}
					fw.flush();
					fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   }	
//************解码**************************************
/*二重纠错：
 * 每16个码字为一个block，每5个block为一个set；
 * 每个block中包含有15码字的码组信息+1码字检验码，该校验码为码组中信息码的异或，接受到后可用信息码异或和该码字对比，相等则解码正确，否者失败；
 * 5个block中最后一个block用来二重纠错，用前面所有的信息码异或的来，5个block中有4个正确，一个错误，则可以用异或求出错误的那个block。
 * */
	if(true)
	{
		rsCode rs2 = new rsCode();
		int[] code2 = Io.readBitAndStranToByte("./result.txt");
		int setNumInAll = BITS_ALL / (NN + 1) / MM / 5;
		//计算共有多少block
		int ValidBlockNum = setNumInAll * 4;
		
		int blockNum = setNumInAll * 5;
		//解码的出的信息码放rawMsg中
		int[] rawMsg = new int[ValidBlockNum * KK];
		/*for(int i = 0; i < 64; i++)
			if(i*3 >= code2.length)
				break;
			else
				code2[i*3] = '?';*/
		int errorNum = 0, errorPos = 0, setNum = 0;

        for(int i = 0, p = 0; i < blockNum; i ++)
        {
        	int[] tmpC = new int[NN];
        	int xor1 = 0;
        	setNum = (i + 1) / 6;//当前block所处的set号
        	if((i+1)%5 == 0)//处理第5个block
        	{
        		System.arraycopy(code2, i * (NN + 1), tmpC, 0, NN);
            	xor1 = code2[i * (NN + 1) + NN];
            	rs2.putToDecode(tmpC);
            	rs2.rsDecode();
            	int[] receive = rs2.getCaledReceive();
            	if(receive[NN - KK] != xor1)
            	{
            		errorNum++;
            		errorPos = (i)%5;
            	}
            	if(errorNum == 1 && errorPos != 4)
            	{
            		int tmpXor = 0;
            		for(int j = 0; j < 5; j++)//利用第5个block，异或来纠错那错误的一个block
            			if(j != errorPos)
            				tmpXor ^= rawMsg[setNum * 5 + j];
            		rawMsg[setNum * 5 + errorPos] = tmpXor;	
            	}
            	errorNum = 0;
            	errorPos = 0;
            	continue;
        	}     	
        	System.arraycopy(code2, i * (NN + 1), tmpC, 0, NN);
        	xor1 = code2[i*(NN + 1) + NN];
        	
        	rs2.putToDecode(tmpC);
        	rs2.rsDecode();
        	int[] receive = rs2.getCaledReceive();
        	if(receive[NN - KK] != xor1)
        	{
        		errorNum++;
        		errorPos = (i+1)%5;
        	}  	
        	System.arraycopy(receive, NN - KK, rawMsg, p++ * KK, KK);
        }
        int[] msgInByte = combination(rawMsg);
        try {
        		byte[] msg = new byte[msgInByte.length];
        		for(int i = 0; i < msg.length; i++)
        			msg[i] = (byte)msgInByte[i];
				FileOutputStream fos = new FileOutputStream("./result_43.txt");
				fos.write(msg);
				String msginstr = msg.toString();
				System.out.println(msginstr);
				fos.flush();
				fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}    
        /*int[] tmp = rs.data.clone();
        int[] tmpbb = rs.bb.clone();
        for(i=0; i<NN-KK; i++)  
            rs.recd[i] = rs.bb[i];  
        for(i=0; i<KK; i++)  
            rs.recd[i+NN-KK] = rs.data[i];  
          
        //主动弄点错误  
        for(i=0; i<2; i++)  
            rs.recd[i] = 11;  
          
        //解码，纠错  
        rs.rsDecode();  
          
        //输出正确编码和纠错后的编码  
        System.out.println("i  data  recd");  
        for(i=0; i<NN-KK; i++) {  
            System.out.println(i + "   " + rs.bb[i] + "   " + rs.recd[i]);  
        }  
        for(i=NN-KK; i<NN; i++) {  
            System.out.println(i + "   " + rs.data[i-NN+KK] + "   " + rs.recd[i]);  
        } */ 
    }  
    private static byte[] combination(byte[] msg1, byte[] msg2)
	{
		byte[] msg = new byte[(msg1.length + msg2.length)/2];
		byte[] merge = new byte[msg1.length + msg2.length];
		System.arraycopy(msg1, 0, merge, 0, msg1.length);
		System.arraycopy(msg2, 0, merge, msg1.length, msg2.length);
		
		for(int i = 0, j = 0; i < merge.length; i += 2, j++)
		{
			msg[j] = (byte) ((merge[i] & 0xf) << 4);
			msg[j] ^= (merge[i + 1] & 0xf) ;
		}
		return msg;
	}
    private static int getXor(int[] is) {
		// TODO Auto-generated method stub
    	int xor = 0;
    	for(int i = 0; i < is.length; i++)
    		xor ^= is[i];
		return xor;
	}

	public static int[] combination(int[] merge)
    {
    	int[] out = new int[merge.length / 2];
    	for(int i = 0, j = 0; i < merge.length; i += 2, j++)
		{
    		out[j] = (byte) (merge[i] & 0xf) << 4;
    		out[j] ^= (merge[i + 1] & 0xf) ;
		}
    	return out;
    }
	static byte[] byteArr2DoubleRadix(int b)
	{
		byte[] s = new byte[MM];

		for(int i = 0; i < MM; i++)
		{
			s[MM - 1 - i] = (byte) (b % 2);
			b = (byte) (b / 2);
		}
		return s;
	}
	static int DoubleRadix2Byte(byte[] d)
	{
		int out = 0;
		for(int i = 0; i < MM; i++)		
			out += d[i] * Math.pow(2, MM - 1 - i);	
		return out;
	}
}  