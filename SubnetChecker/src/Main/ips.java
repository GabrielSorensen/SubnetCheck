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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
public class ips {
	private static FileWriter writer;
	private static BufferedReader reader;
	private static String subnet; //ex: 10.232.224.
	private static String end; //224
	private static String subnetNoDots; //10232224
	private static int target1;
	private static int target2;
	private static boolean debugging = false;
	private static boolean isWMIC;
	private static final String n = System.getProperty("line.separator");
	private static final String N = System.lineSeparator();
	private static final String SLASH = System.getProperty("file.separator");
	private static final String ROOT = System.getProperty("user.dir") + SLASH + "workingdir"+ SLASH;
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
					"This process may take a while. \nThis script also creates a small (<3Mb) folder to work in. \nResults can be found in "+ ROOT + SLASH + "[subnet]");
			System.out.println(ROOT);
			grabCMD();
			setSubnet();
			File file = new File(ROOT + "test.test");
			file.createNewFile();
			file.mkdirs();
			file.deleteOnExit();
			writer = new FileWriter(file);
		} catch (Exception io) {
			System.err.println(io.getMessage());
			io.printStackTrace();
		}
		isWMIC = false;
		//		int g = JOptionPane.showConfirmDialog(null, "Would you like to process WMIC to check for static/dynamic settings?" , "WMIC?", JOptionPane.YES_NO_OPTION);
		//		if (g==0) {
		//			isWMIC = true;
		//		} else {
		//			isWMIC = false;
		//		}
		batch(isWMIC);
		writeCSVtest();
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
				File cmd = new File(System.getenv("WINDIR") + SLASH +"system32"+ SLASH +"cmd.exe");
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
			String SUB = "";
			subnet = "";
			SUB = (String)JOptionPane.showInputDialog(
					null,
					"Enter Subnet to check (ex \"192.168.0.\" )\n",
					"Enter Subnet",
					JOptionPane.PLAIN_MESSAGE,
					null, null,
					"10.0.0.");
			if (debugging) {
				System.err.println(SUB);
			}
			if (SUB == null) {
				JOptionPane.showMessageDialog(null, "Canceled! Exiting.");
				System.exit(0);
			} else if (SUB.substring(0,1).equals("d")||SUB.substring(0,1).equals("D")) {
				debugging = true;
				JOptionPane.showMessageDialog(null, "Debugging turned on.");
				setSubnet();
			} else if (!validate(SUB)) {
				JOptionPane.showMessageDialog(null, "IP did not fit the correct pattern, retry.");
				System.out.println("IP did not fit the correct pattern, retry.");
				setSubnet();
			}
			if (validate(SUB)) {
				if (debugging) {
					System.out.println("validating...");
				}
				subnet = SUB;
				System.out.println("Validated.");
			} 
			if (debugging) {
				System.out.println("Im here");
			}
			String inta = (String)JOptionPane.showInputDialog(
					null,
					"Enter subnet node begining point. (Last Octet)",
					"Starting Node", JOptionPane.PLAIN_MESSAGE,
					null, null,
					"1");
			int int1 = Integer.parseInt(inta);
			if (debugging) {
				System.err.println("beggining point= " + inta);
			}
			String intb = (String)JOptionPane.showInputDialog(
					null,
					"Enter subnet node ending point.",
					"Ending Node",
					JOptionPane.PLAIN_MESSAGE,
					null, null,
					"255");
			int int2 = Integer.parseInt(intb);
			if (debugging) {
				System.err.println("ending point= " + intb);
			}
			if (range(int1, int2)) {
				target1 = int1;
				target2 = int2;
				String s = subnet;
				subnetNoDots = s.replaceAll(".", "");
			} else {
				JOptionPane.showMessageDialog(null, "Range was incorect, retry.");
				setSubnet();
			}
			
		} catch  (Exception e) {
			JOptionPane.showMessageDialog(null, "Failed! , reason: \n" + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static boolean range(int int1, int int2) {
		String a = Integer.toString(int1);
		String b = Integer.toString(int2);
		String pat = "([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
		Pattern patern = Pattern.compile(pat);
		Matcher matcher1 = patern.matcher(a);
		Matcher matcher2 = patern.matcher(b);
		if (matcher1.matches() && matcher2.matches()) {
			if (int2 < int1) {
				System.out.println("Fail1");
				return false;
			}
			System.out.println("range was validated");
			return true;
		} else {
			System.out.println("Fail2");
			return false;
		}
	}
	public static void batch(boolean doWMIC) {
		/**
		 * IF NOT Exist %CD%\225 mkdir %CD%\225 FOR /l %%i IN (0,1,0) DO ( ping -a -n 1 10.232.225.%%i nbtstat -a 10.232.225.%%i ) > %CD%\225\%%i.txt
		 * wmic /Node:10.232.225.199 Path Win32_NetworkAdapterConfiguration Get DHCPEnabled,DNSHostName,MACAddress,IPAddress
		 */
		String p1 = "IF NOT Exist %CD%"+ SLASH +end+" mkdir %CD%"+ SLASH +end;
		String p2 = "FOR /l %%i IN ("+target1+",1,"+target2+") DO (  ";
		String p3 = "\t ping -a -n 1 "+subnet+"%%i ";
		String p4 = "\t nbtstat -a "+subnet+"%%i ";
		String p5 = " > %CD%" + SLASH + subnetNoDots +"results"+ SLASH +"%%i.txt";
		String p6 = ")";
		String wmic = "\t wmic /Node:"+subnet+end+" Path Win32_NetworkAdapterConfiguration Get DHCPEnabled,DNSHostName,MACAddress,IPAddress";
		System.out.println("Writing batch File");
		try {
			File results = new File(ROOT + end + SLASH +"results");
			results.mkdirs();
			if (!results.getParentFile().exists()) {
				results.getParentFile().mkdirs(); //who even knows if this is possible to get into???
			}
			File outTest = new File(ROOT + end);
			System.out.println(ROOT + end);
			outTest.getParentFile().mkdirs();
			outTest.mkdirs();
			if (!outTest.exists() || !outTest.canExecute() || !outTest.canRead() || !outTest.canWrite()) {
				System.err.println("Cant edit file! Trying to fix this.");
				outTest.setExecutable(true);
				outTest.setReadable(true);
				outTest.setWritable(true);
			}
			File file = new File(ROOT + end + "pinger.bat");
			if (!debugging) {
				file.deleteOnExit();
			}
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
			File file = new File(ROOT + end + "pinger.bat");
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
				System.out.println(file.getName() +" absolute path: "+file.getAbsolutePath());
				System.out.println(file.getName() +" canonical path: "+file.getCanonicalPath());
				System.out.println("Parent absolute path: "+parent.getAbsolutePath());
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
			//			int numberOfFilesToWrite = target2 - target1; //This is going to measure progress when we get there.
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




	public static void writeCSVtest() {
		String in = "";
		String name = "";
		String result = "";
		String local = "";
		String netbios = "";
		String wmic = "";
		ArrayList<DataRow> rows = new ArrayList<DataRow>();

		try {

			for (int i=target1; i<target2+1; i++) {
				if (!isWMIC) {
					String s = ROOT + end + "results" + SLASH + i + ".txt";
					System.out.println("results in " + s);
					File outTest = new File(ROOT + end + SLASH + "test.txt");
					outTest.createNewFile();
					outTest.deleteOnExit();
					outTest.mkdirs();
					reader = new BufferedReader(new FileReader(s));
					while ((in = reader.readLine()) != null) {
						System.err.println(in);
						if (in.startsWith("Pinging")) {
							System.out.println("Pinging");
							name = in;
							name = name.trim();
							name = name.replaceAll(",", "");
							name = name.replaceAll("\\{}", " ");
						} else if (in.startsWith("Reply") || in.contains("Request")) {
							System.out.println("Reply//Request");
							result = in;			
							result = result.trim();
							result = result.replaceAll("\\}", "");
							result = result.replaceAll(",", "");
							result = result.replaceAll("\\{}", " ");
						} else if (in.contains("Local Area")) {
							System.out.println("LocalAreaConnection");
							local = in;
							local = local.trim();
							local = local.replaceAll(",", "");
							local = local.replaceAll("\\{}", " ");
						} else if (in.contains("Host") || in.contains("NetBIOS")) {
							System.out.println("NetBIOS");
							netbios = in;
							if (!in.contains("Host")) {
								while (!in.contains("MAC"))  {
									System.err.println(in);
									in = reader.readLine();
									netbios += in;
								}
							}
							netbios = netbios.trim();
							netbios = netbios.replaceAll(n, " ");
							netbios = netbios.replaceAll(",", "");
						} else {
							System.err.println("COULD NOT PLACE LINE! \n" +in+ " <");
						} 
						for (DataRow dat : rows) {
							if (!name.equals(dat.getName())) {
								DataRow row = new DataRow(name, result, local, netbios);
								System.out.println("PLACED: " + row.toStringCommaSeperated());
								rows.add(row);
							} else {
								break;
							}
						}



					} //end while


				} else if (isWMIC) {
					//if wmic switch
				} else {
					JOptionPane.showMessageDialog(null, "Failed! Can't create results file. Error:73");
					System.exit(73);
				}
			}//after looping through results, format to csv
			File file = new File(ROOT + end + SLASH +"result.csv");
			file.createNewFile();
			writer = new FileWriter(file);
			for (DataRow r : rows) {
				writer.append(r.toStringCommaSeperated());
				System.out.println(r.toStringCommaSeperated());
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}
	public static void writeCSV() {
		String in = "";
		String name = "";
		String result = "";
		String local = "";
		String netbios = "";
		String wmic = "";
		ArrayList<DataRow> rows = new ArrayList<DataRow>();
		try {
			for (int i=target1; i<target2+1; i++) {
				if (!isWMIC) {
					String s = ROOT + end + SLASH + "results" + SLASH + i + ".txt";
					if (debugging) {
						System.out.println(s);
					}
					File outTest = new File(ROOT + end + SLASH + "test.txt");
					outTest.createNewFile();
					outTest.mkdirs();
					outTest.deleteOnExit();
					reader = new BufferedReader(new FileReader(s));
					while ((in = reader.readLine()) != null) {
						if (in.startsWith("Pinging")) {
							name = in;
							name = name.trim();
							name = name.replaceAll(",", "");
							name = name.replaceAll("\\{}", " ");
						}
						if (in.startsWith("Reply") || in.contains("Request")) {
							result = in;			
							result = result.trim();
							result = result.replaceAll("\\}", "");
							result = result.replaceAll(",", "");
							result = result.replaceAll("\\{}", " ");
						}
						if (in.contains("Local Area")) {
							local = in;
							local = local.trim();
							local = local.replaceAll(",", "");
							local = local.replaceAll("\\{}", " ");
						}
						if (in.contains("Host") || in.contains("NetBIOS")) {
							netbios = in;
							int j=0;
							while (j!=10)  {
								in = reader.readLine();
								netbios += in;
								j++;
							}
							netbios = netbios.trim();
							netbios = netbios.replaceAll(",", "");
						} 
						if (name != "" && result != "" && local != "" && netbios != "" && wmic != "") {
							DataRow row = new DataRow(name, result, local, netbios);
							rows.add(row);

						}
					}
				} else if (isWMIC) {
					String s = ROOT + end + SLASH + i + ".txt";
					File outTest = new File(ROOT + end + SLASH + "test.txt");
					outTest.createNewFile();
					outTest.mkdirs();
					reader = new BufferedReader(new FileReader(s));
					while ((in = reader.readLine()) != null) {
						if (in.startsWith("Pinging")) {
							name = in;
							name = name.trim();
							name = name.replaceAll(",", "");
							name = name.replaceAll("\\{}", " ");
						}
						if (in.startsWith("Reply") || in.contains("Request")) {
							result = in;			
							result = result.trim();
							result = result.replaceAll("\\}", "");
							result = result.replaceAll(",", "");
							result = result.replaceAll("\\{}", " ");
						}
						if (in.contains("Local Area")) {
							local = in;
							local = local.trim();
							local = local.replaceAll(",", "");
							local = local.replaceAll("\\{}", " ");
						}
						if (in.equals("wmicstuff here")) {
							//  wmicccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc
						}
						if (in.contains("Host") || in.contains("NetBIOS")) {
							netbios = in;
							int j=0;
							while (j!=10)  {
								in = reader.readLine();
								netbios += in;
								j++;
							}
							netbios = netbios.trim();
							netbios = netbios.replaceAll(",", "");
						} 
						if (name != "" && result != "" && local != "" && netbios != "" && wmic != "") {
							DataRow row = new DataRow(name, result, local, netbios);
							rows.add(row);

						}
					}
				} else {
					JOptionPane.showMessageDialog(null, "Failed! Can't create results file. Error:73");
					System.exit(73);
				}
				File file = new File(ROOT + end + SLASH +"result.csv");
				file.createNewFile();
				writer = new FileWriter(file);
				for (DataRow r : rows) {
					writer.append(r.toStringCommaSeperated());
				}
				writer.flush();
				writer.close();
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
			aJProgressBar.setSize(frame.getSize());
			frame.setVisible(true);
		}
		public void destroy() {
			frame.dispose();
		}
	}
	@SuppressWarnings("unused")
	private static class DataRow implements Comparable<DataRow> {
		private final String name;
		private final String reply;
		private final String local;
		private final String host;
		private final String wmic;

		public DataRow(String name, String reply, String local, String host, String wmic) {
			this.name =  name;
			this.reply = reply;
			this.local = local;
			this.host = host.replaceFirst("-", ",");
			this.wmic = wmic;
		}
		public DataRow(String name, String reply, String local, String host) {
			this.name =  name;
			this.reply = reply;
			this.local = local;
			this.host = host.replaceFirst("-", ",");
			this.wmic = "";
		}
		public String getName() {
			return name;
		}
		public String getReply() {
			return reply;
		}
		public String getLocal() {
			return local;
		}
		public String getHost() {
			return host;
		}
		public String getWmic() {
			return wmic;
		}
		public boolean equals (Object o) {
			if (!(o instanceof DataRow)) {
				return false;
			}
			DataRow r = (DataRow) o;
			return r.getName().equals(this.name);
		}
		public int hashCode() {
			return name.hashCode();
		}
		@Override
		public int compareTo(DataRow data) {
			return (this.name.compareTo(data.getName()));
		}
		public String toStringCommaSeperated() {
			String s = name;
			s +=",";
			s += reply;
			s +=",";
			s += host;
			s +=",";
			s += wmic;
			s += n;
			return s;
		}
	}
}