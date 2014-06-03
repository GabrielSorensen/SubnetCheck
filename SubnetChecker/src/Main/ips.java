package Main;
import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
public class ips {
	private static FileWriter writer;
	private static BufferedReader reader;
	private static String SUBNET; //ex: 10.232.224.
	private static String END; //224
	private static int target1;
	private static int target2;
	private static boolean debugging = true;
	private static boolean isWMIC;
	private static final String n = System.getProperty("line.separator");
	private static final String N = System.lineSeparator();
	private static final String ROOT = System.getProperty("user.dir") + "\\workingdir\\";
	private static final String OS_NAME = System.getProperty("os.name");
	private static final String JAVAVERSION = System.getProperty("java.version");
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	private static final String IPPattern = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.";

	public static void main(String[] args) {
		System.out.println("Using Java Version: "+JAVAVERSION);
		System.out.println("Grabbed version: " + Double.parseDouble(JAVAVERSION.substring(0, 3)));
		if (Double.parseDouble(JAVAVERSION.substring(0, 3)) < 1.6) {
			JOptionPane.showMessageDialog(null, "Current installed java version is: "+JAVAVERSION+". \nThis appleication requires at least version 6. \nExiting.");
			System.exit(13);
		}
		try {
			JOptionPane.showMessageDialog(
					null, 
					"This process may take a while. \nThis script also creates a small (<3Mb) folder to work in. \nResults can be found in "+ ROOT + "\\[subnet]");
			System.out.println(ROOT);
			grabCMD();
			setSubnet();
			File file = new File(ROOT + "test.test");
			file.createNewFile();
			file.mkdirs();
			writer = new FileWriter(file);
			Thread.sleep(1000);
			file.deleteOnExit();
		} catch (Exception io) {
			System.err.println(io.getMessage());
			io.printStackTrace();
		}
		int g = JOptionPane.showConfirmDialog(null, "Would you like to process WMIC to check for static/dynamic settings?" , "WMIC?", JOptionPane.YES_NO_OPTION);
		if (g==0) {
			isWMIC = true;
		} else {
			isWMIC = false;
		}
		batch(isWMIC);
		//		writeCSV();
		System.out.println("Done!");
		int n = JOptionPane.showConfirmDialog(null, "Process Completed, try another subnet?" , "Finished", JOptionPane.YES_NO_OPTION);
		//yes n=0 no n=1
		if (n==0) {
			main(args);
		} else {
			System.exit(0);
		}
	}
	@SuppressWarnings("resource")
	public static void grabCMD() {
		try {
			if (OS_NAME.contains("win") || OS_NAME.contains("Win")) {
				File cmd = new File(System.getenv("WINDIR") + "\\system32\\cmd.exe");
				File javaCmd = new File(ROOT + "JavaC.exe");
				if(!javaCmd.exists()) {
					javaCmd.getParentFile().mkdirs();
					javaCmd.createNewFile();
				}
				javaCmd.deleteOnExit();
				FileChannel source = null;
				FileChannel destination = null;
				source = new FileInputStream(cmd).getChannel();
				destination = new FileOutputStream(javaCmd).getChannel();
				destination.transferFrom(source, 0, source.size());
				source.close();
				destination.close();
			} else if (OS_NAME.contains("OSX")) {
				JOptionPane.showMessageDialog(null, "This process is currently only supported on Windows machines.");
				System.out.println("Macs will not be supported.");
				System.exit(13);
			} else {
				JOptionPane.showMessageDialog(null, "This process is currently only supported on Windows machines.");
				// work on getting this for Linux based machines
				System.exit(13);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void setSubnet() {
		try {
			String SUB = (String)JOptionPane.showInputDialog(
					null,
					"Enter Subnet to check (ex \"192.168.0.\" )\n",
					"Enter Subnet",
					JOptionPane.PLAIN_MESSAGE,
					null, null,
					"10.0.0.");
			if (SUB == null) {
				JOptionPane.showMessageDialog(null, "Canceled! Exiting.");
				System.exit(0);
			}
			if (validate(SUB)) {
				SUBNET = SUB;
			} else {
				JOptionPane.showMessageDialog(null, "IP did not fit the correct pattern, retry.");
				System.out.println("IP did not fit the correct pattern, retry.");
				setSubnet();
			}
			String inta = (String)JOptionPane.showInputDialog(
					null,
					"Enter subnet node begining point. (Last Octet)",
					"Starting Node", JOptionPane.PLAIN_MESSAGE,
					null, null,
					"1");
			int int1 = Integer.parseInt(inta);
			while (int1 < 0 || int1 > 255) {
				inta = (String)JOptionPane.showInputDialog(
						null,
						"Enter subnet node begining point. (Last Octet)",
						"Starting Node", JOptionPane.PLAIN_MESSAGE,
						null, null,
						"1");
				int1 = Integer.parseInt(inta);
			}
			String intb = (String)JOptionPane.showInputDialog(
					null,
					"Enter subnet node ending point.",
					"Ending Node",
					JOptionPane.PLAIN_MESSAGE,
					null, null,
					"255");

			int int2 = Integer.parseInt(intb);
			while ( int2 < int1 || int2 > 255) {
				intb = (String)JOptionPane.showInputDialog(
						null,
						"Enter subnet node ending point.",
						"Ending Node",
						JOptionPane.PLAIN_MESSAGE,
						null, null,
						"255");
				int2 = Integer.parseInt(intb);
			}
			target1 = int1;
			target2 = int2;
			if ((SUB != null) && (SUB.length() > 0)) {
				String reverse = new StringBuilder(SUB).reverse().toString();
				String [] sa = reverse.split("\\.");
				if (debugging) {
					END = new StringBuilder(sa[0]).reverse().toString();
				}
				return;
			} else {
				JOptionPane.showMessageDialog(null, "Failed! Can't set subnet! Exiting.");
				System.exit(-1);
			}
		} catch  (Exception e) {
			JOptionPane.showMessageDialog(null, "Failed! , reason: \n" + e.getMessage());
			e.printStackTrace();
		}
	}
	public static void batch(boolean doWMIC) {
		/**
		 * IF NOT Exist %CD%\225 mkdir %CD%\225 FOR /l %%i IN (0,1,0) DO ( ping -a -n 1 10.232.225.%%i nbtstat -a 10.232.225.%%i ) > %CD%\225\%%i.txt
		 * wmic /Node:10.232.225.199 Path Win32_NetworkAdapterConfiguration Get DHCPEnabled,DNSHostName,MACAddress,IPAddress
		 */
		String p1 = "IF NOT Exist %CD%\\"+END+" mkdir %CD%\\"+END;
		String p2 = "FOR /l %%i IN ("+target1+",1,"+target2+") DO (  ";
		String p3 = "\t  ping -a -n 1 "+SUBNET+"%%i ";
		String p4 = "\t nbtstat -a "+SUBNET+"%%i ";
		String p5 = " > %CD%\\"+ SUBNET +"\\ " +END+ "%%i.txt";
		String p6 = ")";
		String wmic = "wmic /Node:"+SUBNET+END+" Path Win32_NetworkAdapterConfiguration Get DHCPEnabled,DNSHostName,MACAddress,IPAddress";
		System.out.println("Writing batch File");
		try {
			File outTest = new File(ROOT + END);
			System.out.println(ROOT + END);
			outTest.getParentFile().mkdirs();
			outTest.mkdirs();
			if (!outTest.exists() || !outTest.canExecute() || !outTest.canRead() || !outTest.canWrite()) {
				System.err.println("Cant edit file! Trying to fix this.");
				outTest.setExecutable(true);
				outTest.setReadable(true);
				outTest.setWritable(true);
			}
			File file = new File(ROOT + END + "pinger.bat");
			//			file.deleteOnExit();
			writer = new FileWriter(file);
			writer.write(p1);
			writer.append(n);
			writer.append(p2);
			writer.append(n);
			writer.append(p3);
			writer.append(n);
			writer.append(p4);
			writer.append(n);
			if (doWMIC) {
				writer.append(wmic);
				writer.append(N);	
			}
			writer.append(p6);
			if (debugging) {
				writer.append(p5);
			}
			writer.append(n);
			writer.flush();
			writer.close();
		} catch (Exception io) {
			JOptionPane.showMessageDialog(null, "Process failed, reason: \n" + io.getMessage());

		}
		try {
			File file = new File(ROOT + END + "pinger.bat");
			while (!file.exists()) {
				int k=0;
				Thread.sleep(5000);
				k++;
				if (k>13) {
					System.out.println("Process failed! Error Code:5");
					JOptionPane.showMessageDialog(null, "Failed! Can't run batchfile. Error:5");
				}
			}
			System.out.println("Running batchfile");
			File parent = file.getParentFile();
			if (debugging) {
				System.out.println(file.getAbsolutePath());
				System.out.println(file.getCanonicalPath());
				System.out.println(parent.getAbsolutePath());
			}
			Process p = Runtime.getRuntime().exec(file.getAbsolutePath(),(String[]) null, parent);
			GProgressBar monitor = new GProgressBar();
			GProgressBar.main(null);
			InputStream is = p.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line=null;
			line = br.readLine();				
			Date today = Calendar.getInstance().getTime();
			int numberOfFilesToWrite = target2 - target1;
			while (p.isAlive()) {
				//				Thread.sleep(3000);

				if (br.readLine() != line) {
					String reportDate = DATE_FORMAT.format(today);
					line = br.readLine();
					if (debugging) {
						System.out.println("Report Date: " + reportDate + line);
					}
				}
			}
			if (p.exitValue()!=0) {
				System.out.println("Process failed! Error Code: "+p.exitValue());
				System.exit(p.exitValue());
			}
			System.out.println("Killing batch process.");
			monitor.destroy();
			p.destroy();
		} catch (Exception iTripped) {
			JOptionPane.showMessageDialog(null, "Error running commands, aborting. \n Reason: " + iTripped.getMessage());
		}
	}
	public static boolean validate(String ip) {
		Pattern pat = Pattern.compile(IPPattern);
		Matcher match = pat.matcher(ip);
		return match.matches();
	}
	public static void writeCSV() {
		String in = "";
		String na = "";
		String reply = "";
		Map <String, String> mapping = new LinkedHashMap<String, String>(); 
		try {
			for (int i=1; i<255; i++) {
				if (debugging) {
					String s = ROOT + END + "\\" + i + ".txt";
					File outTest = new File(ROOT + END + "\\test.txt");
					outTest.createNewFile();
					outTest.mkdirs();
					reader = new BufferedReader(new FileReader(s));
					while ((in = reader.readLine()) != null) {
						if (in.contains("Pinging")) {
							na = in;
							na = na.trim();
						}
						if (in.contains("Request") || in.contains("Reply") ) {
							in = reader.readLine();
							reply = in;			
							reply = reply.trim();
							reply = reply.replaceAll("\\}", "");
							in = reader.readLine();
							in = reader.readLine();
						}
						if ((na != null && reply != null) || na!="" || reply != "" || na!=" " || reply != " ") {
							if (mapping.containsKey(na)) {
								continue;
							} else {
								mapping.put(na, reply);
							}

						}
						String unclean = mapping.toString();
						unclean = unclean.replaceAll("\\{}", " ");
						ArrayList<String> cleanMe = new ArrayList<String>();
						String [] a = unclean.split(",");
						for (String b : a) {
							if (b.startsWith("{")) {
								continue;
							}
							b = b.replace("=", "+");
							b = b.trim();
							b = b.replace("+", ",");
							b = b.replaceAll("Pinging", "");
							b = b.replaceAll("with 32 bytes of data:", "");
							b = b.replaceAll("Reply from ", "");
							cleanMe.add(b);
						}
						File file = new File(ROOT + END +"\\result.csv");
						file.createNewFile();
						writer = new FileWriter(file);
						for (String b : cleanMe) {
							writer.write(b);
						}
						writer.flush();
						writer.close();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static class GProgressBar {
		private static JFrame frame;
		public static void main(String args[]) {
			frame = new JFrame("Running..");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			final JProgressBar aJProgressBar = new JProgressBar();
			aJProgressBar.setStringPainted(false);
			aJProgressBar.setIndeterminate(true);
			frame.add(aJProgressBar, BorderLayout.NORTH);
			frame.setSize(300, 100);
			frame.setVisible(true);
		}
		public void destroy() {
			frame.dispose();
		}
	}
}