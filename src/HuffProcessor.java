
/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */
import java.util.*;
public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
		int[] counts=readForCounts(in);
		HuffNode root=makeTree(counts);
		String[] codings=makeCodings(root);
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root,out);
		in.reset();
		writeCompressed(codings,in,out);
		out.close();
	}
	private void writeCompressed(String[] codings, BitInputStream in, BitOutputStream out) {
		// TODO Auto-generated method stub
		while(true) {
			int val= in.readBits(BITS_PER_WORD);
			if(val==-1)break;
			String code= codings[val];
			out.writeBits(code.length(),Integer.parseInt(code,2));
		}
		String pe=codings[PSEUDO_EOF];
		out.writeBits(pe.length(), Integer.parseInt(pe,2));
	}

	private void writeHeader(HuffNode root, BitOutputStream out) {
		// TODO Auto-generated method stub
		if(root.myLeft!=null && root.myRight!=null) {
			out.writeBits(1,0);
			writeHeader(root.myLeft,out);
			writeHeader(root.myRight,out);
		}
		else {
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD+1, root.myValue);
		}
	}

	private String[] makeCodings(HuffNode root) {
		// TODO Auto-generated method stub
		String[] code=new String[ALPH_SIZE +1];
		changer(root,"",code);
		return code;
	}

	private void changer(HuffNode root, String string, String[] code) {
		// TODO Auto-generated method stub
		if(root.myLeft==null && root.myRight==null) {
			code[root.myValue]=string;
			return ;
		}
		changer(root.myRight,string+"1",code);
		changer(root.myLeft,string+"0",code);
	}

	private HuffNode makeTree(int[] counts) {
		// TODO Auto-generated method stub
		PriorityQueue<HuffNode> pq= new PriorityQueue<>();
		for(int i=0;i<counts.length;i++) {
			if(counts[i]>0) {
				pq.add(new HuffNode(i,counts[i],null,null));
			}
		}
		while(pq.size()>1) {
			HuffNode left=pq.remove();
			HuffNode right=pq.remove();
			HuffNode a=new HuffNode(0,left.myWeight+right.myWeight,left,right);
			pq.add(a);
		}
		HuffNode root=pq.remove();
		return root;
	}

	private int[] readForCounts(BitInputStream in) {
		// TODO Auto-generated method stub
		int[] counts=new int[ALPH_SIZE + 1];
		while(true) {
			int countV=in.readBits(BITS_PER_WORD);
			if(countV==-1) {
				break;
			}
			counts[countV]++;
		}
		counts[PSEUDO_EOF]=1;
		return counts;
	}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
		int val = in.readBits(BITS_PER_INT);
		if(val != HUFF_TREE || val==-1) {
			throw new HuffException("Illegal header starts with" +val);
		}
		HuffNode root=readHeader(in);
		readCompressed(root,in,out);
		out.close();
	}

	private void readCompressed(HuffNode root, BitInputStream in, BitOutputStream out) {
		// TODO Auto-generated method stub
		HuffNode curr=root; 
		while(true) {
			int val=in.readBits(1);
			if(val==-1) {
				throw new HuffException("no PSEUDO_EOF");
			}
			else {
				if(val==0) {
					curr=curr.myLeft;
				}
				else {
					curr=curr.myRight;
					if(curr.myLeft==null && curr.myRight==null) {
						if(curr.myValue==PSEUDO_EOF) {
							break;
						}
						else {
							out.writeBits(BITS_PER_WORD, curr.myValue);
							curr=root;
						}
					}
				}
			}
		}
	}

	private HuffNode readHeader(BitInputStream in) {
		// TODO Auto-generated method stub
		int val =in.readBits(1);
		if(val==-1) {
			throw new HuffException("Illegal header starts with" +val);
		}
		if(val==0) {
			HuffNode left=readHeader(in);
			HuffNode right=readHeader(in);
			return new HuffNode(0,0,left,right);
		}
		else {
			int newVal=in.readBits(BITS_PER_WORD+1);
			return new HuffNode(newVal,0,null,null);
		}
	}
}