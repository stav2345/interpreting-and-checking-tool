package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import business_rules.TermRules;
import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import dcf_manager.Dcf.DcfType;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import utilities.GlobalUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The class uses the BR provided by the Catalogue browser in order to interpret
 * FoodEx2 codes and check their correctness
 * 
 * @author shahaal
 */
public class ICT extends TermRules {
	
	private static final Logger LOGGER = LogManager.getLogger(ICT.class);

	/**
	 * Start the program by command line used from ICT
	 * 
	 * @param argv
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {

		try {

			// argument checks
			if (args.length != 5) {
				LOGGER.error("ERROR!\n Wrong number of parameters passed to app.jar. Expected 5, found" + args.length);

				// wait before close
				Thread.sleep(2000);
				return;
			}

			// set the working directory to find files
			String workingDir = args[2];
			GlobalUtil.setWorkingDirectory(workingDir);

			File input = new File(args[0]);

			String mtxCode = args[3];
			boolean local = getBoolean(args[4]);

			// start the warning utils with the mtx catalogue
			ICT warnUtil = new ICT(mtxCode, local);

			// output file (it will capture all the standard output)
			PrintStream out = new PrintStream(new FileOutputStream(args[1]));
			System.setOut(out); // redirect standard output to the file

			// read the codes from the input file
			FileReader reader = new FileReader(input);
			BufferedReader buffReader = new BufferedReader(reader);

			int lineCount = 0;

			LOGGER.error("\n+++++++++++++++++++ ANALYZING FOODEX2 CODES +++++++++++++++++++\n");

			// read the number of lines for the progress bar
			Path path = Paths.get(input.getAbsolutePath());
			long totLines = Files.lines(path, StandardCharsets.ISO_8859_1).count();

			// try-with-resource block
			try (ProgressBar pb = new ProgressBar("Analysing", totLines, ProgressBarStyle.ASCII)) {
				String line;
				// for each code perform the warning checks
				while ((line = buffReader.readLine()) != null) {

					// add a separator among the warnings related to different codes
					if (lineCount != 0)
						LOGGER.info("\n");

					// perform the warnings checks for the current code
					warnUtil.performWarningChecks(line, true, true);
					lineCount++;
					// print progress bar
					pb.step();
				}
			}

			// close the input file
			buffReader.close();
			// close the output file
			out.close();
			LOGGER.error("\nAll the FoodEx2 codes has been analysed!");

		} catch (Exception e) {
			LOGGER.error("\nERROR in Main!\n" + e.getMessage());
			e.printStackTrace();
		}

		// wait before close
		Thread.sleep(2000);

	}

	public ICT(String mtxCode, boolean local) throws ICT.MtxNotFoundException, InterruptedException {
		CatalogueDAO catDao = new CatalogueDAO();
		DcfType type = local ? DcfType.LOCAL : DcfType.PRODUCTION;

		Catalogue mtx = catDao.getLastVersionByCode(mtxCode, type);

		if (mtx == null) {
			throw new MtxNotFoundException(mtxCode, type);
		}
		currentCat = mtx;

		LOGGER.error("Loading catalogue data into RAM...");

		currentCat.loadData();

		loadFileData();
	}

	public class MtxNotFoundException extends FileNotFoundException {
		/**
		 * 
		 */
		private static final long serialVersionUID = -45955835909711360L;

		public MtxNotFoundException(String mtxCode, DcfType type) {
			super();
		}
	}

	private void loadFileData() throws InterruptedException {

		forbiddenProcesses = loadForbiddenProcesses(GlobalUtil.getBRData());
		warningMessages = loadWarningMessages(GlobalUtil.getBRMessages());
	}

	/**
	 * Print the warning messages
	 * 
	 * @param event
	 * @param postMessageString
	 * @param attachDatetime
	 * @param stdOut
	 */
	protected void printWarning(WarningEvent event, String postMessageString, boolean attachDatetime, boolean stdOut) {

		// create the warning message to be printed
		String message = createMessage(event, postMessageString, attachDatetime);

		// get the warning levels for making colours
		WarningLevel semaphoreLevel = getSemaphoreLevel(event);
		WarningLevel textWarningLevel = getTextLevel(event);

		// do not print the base term successfully added warning if in stdout
		if (stdOut && event != WarningEvent.BR22) {

			StringBuilder sb = new StringBuilder();
			sb.append(message);
			sb.append(";");
			sb.append(semaphoreLevel.toString());
			sb.append(";");
			sb.append(textWarningLevel.toString());

			// print the line
			LOGGER.info(sb.toString() + "|");
			return;
		}
	}

	/**
	 * Return true if the string value is either true or 1, otherwise false.
	 * 
	 * @param value
	 * @return
	 */
	private static boolean getBoolean(String value) {
		if (value.equals("true") || value.equals("1") || value.equals("YES"))
			return true;
		else
			return false;
	}

	@Override
	protected boolean highWarningsPresent() {
		// TODO Auto-generated method stub
		return false;
	}
}
