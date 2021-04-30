package com.vitimage.fijirelax.gui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import com.phenomen.common.VitiDialogs;
import com.phenomen.common.VitimageUtils;
import com.vitimage.fijirelax.mrialgo.HyperMap;
import com.phenomen.fijiyama.RegistrationAction;
import com.phenomen.fijiyama.RegistrationManager;
import com.phenomen.registration.OptimizerType;
import com.phenomen.registration.Transform3DType;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.Memory;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;

/**
 * A class which run the FijiRelax GUI
 * @author Romain Fernandez (romain.fernandez@cirad.fr)
 * @version 1.0, 11.02.2021
 * 
 * 
 */
public class FijiRelax_Gui extends PlugInFrame  implements ActionListener {

	//Buttons, Frames, Panels
	//Registration Frame buttons
	public JButton importButton=new JButton("Import Nifti / Dicom data");
	private JButton openButton=new JButton("Open existing hypermap");
	private JButton registerButton=new JButton("Register sequences");
	private JButton computeButton=new JButton("Compute maps");
	private JButton sosButton = new JButton("About FijiRelax...");
	private JButton explorerButton=new JButton("Explorer");
	private JButton undoButton=new JButton("Undo");
	private JButton abortButton=new JButton("Abort");
	private JButton stressButton=new JButton("Feeling stressed with FijiRelax ?");
	private JButton outliersButton=new JButton("Process outliers");

	//Some more Gui objects and constants
	public JFrame registrationFrame;
	private JTextArea logArea=new JTextArea("", 11,10);
	private Color colorIdle;
	private int screenHeight=0;
	private int screenWidth=0;
	public int[] lastViewSizes=new int[] {700,700};//Only useful for serie running
	public final String displayedNameImage1="Image 1";
	public final String displayedNameImage2="Image 2";
	public final String displayedNameCombinedImage="Data_combined";
	public final String displayedNameHyperImage="Data_combined";

	private Font mySurvivalFontForLittleDisplays=null;

	public String versionName="Handsome honeysuckle";
	public String timeVersionFlag="  Release time : 2020-02-12 - 08:35 PM";
	public String versionFlag=versionName+timeVersionFlag;
	public ImagePlus imgView;
	private Color colorStdActivatedButton;
	private Color colorGreenRunButton;
	public boolean interfaceIsRunning=false;

	private static final String saut="<div style=\"height:1px;display:block;\"> </div>";
	private static final String startPar="<p  width=\"650\" >";
	private static final String nextPar="</p><p  width=\"650\" >";

	//Identifiers for buttons
	private static final int IMPORT=91;
	private static final int OPEN=92;
	private static final int REGISTER=93;
	private static final int COMPUTE=94;
	private static final int EXPLORER=95;
	private static final int SOS=96;
	private static final int ABORT=97;
	private static final int STRESS=98;
	private static final int UNDO=99;
	private static final int OUTLIERS=100;

	private volatile boolean actionAborted=false;
	boolean developerMode=false;
	String spaces="                                                                                                                                   ";
	public int viewSlice;

	private boolean debugMode=true;
	private boolean isSurvivorVncTunnelLittleDisplay=false;
	private int nbCpu;
	private int jvmMemory;
	private long memoryFullSize;
	private HyperMap currentHypermap;
	private ArrayList<HyperMap>hypermapList=new ArrayList<HyperMap>();
	private ArrayList<ImagePlus>imgShowedList=new ArrayList<ImagePlus>();
	private boolean lock=false;
	private int step=0;

	private static final long serialVersionUID = 1L;

	/** Entry points and startup functions--------------------------------------------------------------------------------*/
	public static void main(String[]args) {		
		ImageJ ij=new ImageJ();
		FijiRelax_Gui fj=new FijiRelax_Gui();
		fj.run("");
		fj.automaticTest();
	}

	public void automaticTest() {
//		startFromTestImage("/home/fernandr/Bureau/test.tif");
		startFromTestImage("/home/fernandr/Bureau/Traitements/Sorgho/Series_temporelles/All_timeseries/BM1_Timeseries.tif");
		//startFromTestImage("/home/fernandr/Bureau/FijiRelax_PrepaDOI/Tests_Refactoring/3_Computed_Maps/hyper.tif");
//		startFromTestImage("/home/fernandr/Bureau/FijiRelax_PrepaDOI/Tests_Refactoring/1_Imported/hyper.tif");
	}
	
	public FijiRelax_Gui() {
		super("");
		this.screenHeight=Toolkit.getDefaultToolkit().getScreenSize().height;
		this.screenWidth=Toolkit.getDefaultToolkit().getScreenSize().width;
		if(this.screenWidth>1920)this.screenWidth/=2;
	}

	public void run(String arg) {
		if(new File("/users/bionanonmri/").exists()) {
			this.isSurvivorVncTunnelLittleDisplay=true;
			IJ.showMessage("Detected Bionano server. \nSurvival display, but numerous cores");
		}
		startFijiRelaxInterface();
		welcomeAndInformAboutComputerCapabilities();
		
	}
	
	public void startFijiRelaxInterface() {
		IJ.log("Starting FijiRelax registration interface");

		//Panel with console-style log informations and requests
		JPanel consolePanel=new JPanel();
		consolePanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		consolePanel.setLayout(new GridLayout(1,1,0,0));
		logArea.setSize(isSurvivorVncTunnelLittleDisplay ? 400 : 600,isSurvivorVncTunnelLittleDisplay ?  57 : 80);
		logArea.setBackground(new Color(10,10,10));
		logArea.setForeground(new Color(245,255,245));
		logArea.setFont(new Font(Font.DIALOG,Font.PLAIN,isSurvivorVncTunnelLittleDisplay ? 10 : 14));
		JScrollPane jscroll=new JScrollPane(logArea);
		jscroll.setVisible(true);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setEditable(false);	
        consolePanel.add(jscroll);

       //Panel with step settings, used for registration of two images, and when programming registration pipelines for series
		JPanel stepSettingsPanel=new JPanel();
		mySurvivalFontForLittleDisplays=null;
		if(isSurvivorVncTunnelLittleDisplay ) {
			String name=openButton.getFont().getFamily();
			mySurvivalFontForLittleDisplays=new Font(name,Font.BOLD,11);
			importButton.setFont(mySurvivalFontForLittleDisplays);
			openButton.setFont(mySurvivalFontForLittleDisplays);
			registerButton.setFont(mySurvivalFontForLittleDisplays);
			computeButton.setFont(mySurvivalFontForLittleDisplays);
			sosButton.setFont(mySurvivalFontForLittleDisplays);
			undoButton.setFont(mySurvivalFontForLittleDisplays);
			stressButton.setFont(mySurvivalFontForLittleDisplays);
			outliersButton.setFont(mySurvivalFontForLittleDisplays);
		}



		
		
		//Buttons panels
		JPanel []buttonsPanel=new JPanel[4];
		for(int pan=0;pan<4;pan++) {
			buttonsPanel[pan]=new JPanel();
			if(isSurvivorVncTunnelLittleDisplay ) {
				buttonsPanel[pan].setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
			}
			else {
				buttonsPanel[pan].setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
			}
		}
		buttonsPanel[0].setLayout(new GridLayout(1,2,70,5));		
		buttonsPanel[0].add(importButton);
		buttonsPanel[0].add(openButton);

		buttonsPanel[1].setLayout(new GridLayout(1,3,30,5));
		buttonsPanel[1].add(registerButton);
		buttonsPanel[1].add(computeButton);
		buttonsPanel[1].add(outliersButton);

		buttonsPanel[2].setLayout(new GridLayout(1,1,70,5));
		buttonsPanel[2].add(explorerButton);
		
		buttonsPanel[3].setLayout(new GridLayout(3,2,70,5));
		buttonsPanel[3].add(undoButton);
		buttonsPanel[3].add(abortButton);
		buttonsPanel[3].add(new JLabel(""));
		buttonsPanel[3].add(new JLabel(""));
		buttonsPanel[3].add(sosButton);
		buttonsPanel[3].add(stressButton);
	
		this.colorIdle=abortButton.getBackground();
		colorStdActivatedButton=openButton.getBackground();
		colorGreenRunButton=new Color(100,255,100);
		
		importButton.addActionListener(this);
		explorerButton.addActionListener(this);
		openButton.addActionListener(this);
		registerButton.addActionListener(this);
		computeButton.addActionListener(this);
		sosButton.addActionListener(this);
		undoButton.addActionListener(this);
		stressButton.addActionListener(this);
		outliersButton.addActionListener(this);
		abortButton.addActionListener(this);

		importButton.setToolTipText("<html><p width=\"500\">" +	"Import dicom dir, nifti 4D or series of nifti 3D."+"</p></html>");
		openButton.setToolTipText("<html><p width=\"500\">" +	"Open a previously imported Hypermap from 4D nifti or 4D tif file."+"</p></html>");
		registerButton.setToolTipText("<html><p width=\"500\">" +	"Register T1 / T2 series"+"</p></html>");
		computeButton.setToolTipText("<html><p width=\"500\">" +	"Compute PD, T1 and/or T2 maps."+"</p></html>");
		abortButton.setToolTipText("<html><p width=\"500\">" +	"Interruption of current process"+"</p></html>");
		undoButton.setToolTipText("<html><p width=\"500\">" +	"Undo last action"+"</p></html>");
		explorerButton.setToolTipText("<html><p width=\"500\">" +	"Run explorer on the current hypermap"+"</p></html>");
		sosButton.setToolTipText("<html><p width=\"500\">" +	"Display Sos window"+"</p></html>");
		stressButton.setToolTipText("<html><p width=\"500\">" +	"Feeling stressed with FijiRelax ?"+"</p></html>");
		outliersButton.setToolTipText("<html><p width=\"500\">" +	"Detect and process outliers in maps"+"</p></html>");

		JPanel registrationPanelGlobal=new JPanel();
		if(isSurvivorVncTunnelLittleDisplay )registrationPanelGlobal.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		else registrationPanelGlobal.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
		registrationPanelGlobal.setLayout(new BoxLayout(registrationPanelGlobal, BoxLayout.Y_AXIS));
		String[]panelText=new String[] {"Open data","Process data","Graphical explorer","Need help ?"};
		for(int pan=0;pan<4;pan++) {
			registrationPanelGlobal.add(new JSeparator());
			JPanel tmpPanel=new JPanel();
			tmpPanel.setLayout(new GridLayout(2,1,10,10));
			tmpPanel.add(new JLabel(panelText[pan]));
			tmpPanel.add(new JLabel(""));
			registrationPanelGlobal.add(tmpPanel);			
			registrationPanelGlobal.add(buttonsPanel[pan]);
			registrationPanelGlobal.add(new JLabel(""));
			registrationPanelGlobal.add(new JLabel(""));
		}

	
		//Main frame and main panel		
		registrationFrame=new JFrame();
		registrationFrame.setLayout(new BoxLayout(registrationFrame.getContentPane(), BoxLayout.Y_AXIS));
		registrationFrame.add(consolePanel);
		registrationFrame.add(registrationPanelGlobal);
		registrationFrame.setTitle("FijiRelax ");
		registrationFrame.pack();
		if(isSurvivorVncTunnelLittleDisplay ) registrationFrame.setSize(600,680);
		else registrationFrame.setSize(750,850);
		registrationFrame.setResizable(false);
		
		registrationFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		registrationFrame.addWindowListener(new WindowAdapter(){
             public void windowClosing(WindowEvent e){
                   IJ.showMessage("See you next time !");
                   registrationFrame.setVisible(false);
                   closeAllViews();
               }
		});
		registrationFrame.setVisible(true);
		VitimageUtils.adjustFrameOnScreen(registrationFrame,2,0);		
		
		disable(new int[] {REGISTER,COMPUTE,ABORT,UNDO,EXPLORER,OUTLIERS});
		enable(new int[] {OPEN,IMPORT,SOS,STRESS});
	}

	
	
	
	
	/** Performed actions called from the interface --------------------------------------------------------------------------------*/	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==this.abortButton)runActionAbort();
		if(e.getSource()==this.sosButton)runActionSos();
		if(e.getSource()==this.stressButton)runActionStress();
		if(lock)return;
		if(e.getSource()==this.importButton)runActionImport();
		if(e.getSource()==this.openButton)runActionOpen();
		if(e.getSource()==this.registerButton)runActionRegistration();
		if(e.getSource()==this.computeButton)runActionCompute();
		if(e.getSource()==this.undoButton)runActionUndo();
		if(e.getSource()==this.explorerButton)runActionExplorer();
		if(e.getSource()==this.outliersButton)runActionOutliers();
	}
	
	public void runActionImport() {		
		this.addLog("Import image.", 1);
		VitiDialogs.notYet("runActionImport");		
		if(this.currentHypermap.T>1)disable(REGISTER);
	}

	public void runActionRegistration() {
 		if(currentHypermap==null)return;
		this.addLog("Register series.", 1);
		HyperMap tmp=HyperMap.hyperMapFactory(this.currentHypermap);
		RegistrationAction regAct=openRegistrationSettingsDialog();
		if(regAct==null) {addLog("Registration cancelled", 0);return;}
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		exec.submit(new Runnable() {
			public void run()  {					lock();	tmp.registerEchoes(regAct);	  		step++; 	hypermapList.add(tmp);		   		currentHypermap=tmp;	updateView();unlock();}
		});
	}

	public void runActionCompute() {
 		if(currentHypermap==null)return;
		this.addLog("Compute maps.", 1);
		HyperMap tmp=HyperMap.hyperMapFactory(this.currentHypermap);
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		exec.submit(new Runnable() {
			public void run()  {lock();tmp.computeMaps();	  		step++; 	hypermapList.add(tmp);	   		currentHypermap=tmp;		updateView();unlock();}
		});
	}
	
	public void runActionOutliers() {
		if(currentHypermap==null)return;
		this.addLog("Removing outliers.", 1);
		int oneForTukeyFenceTwoForMADe=2;
   		double nStdDev=5;
   		int blockHalfSize=4;
   		HyperMap tmp=HyperMap.hyperMapFactory(currentHypermap);
   		if(VitiDialogs.getYesNoUI("Simulate ?", "Do you want to simulate on an image crop ?")) {
   			final ExecutorService exec = Executors.newFixedThreadPool(1);
   			exec.submit(new Runnable() {
   				public void run()  {	
   					lock();
   					int cropRad=50;
		   			int cropX=tmp.X/2;
		   			int cropY=tmp.Y/2;
		       		ImagePlus res=tmp.simulateOutlierRemoval(cropX-cropRad,cropY-cropRad,cropX+cropRad,cropY+cropRad,viewSlice,viewSlice);
		       		IJ.showMessage("Outlier removal results with various parameters (Z slicer to change parameters)\nParameters are written in the image label (top line)");
		       		res.show();
		       		res.setTitle("Outlier removal simulation");
		       		res.setPosition(VitimageUtils.getCorrespondingSliceInHyperImage(res, 0, 1, 0));
		       		VitimageUtils.setImageWindowSizeTo(res,500);
		       		unlock();
   		   		}
   			});
   			return;
   		}
   			
   		
   		double[]params=openOutliersDialog();
   		if(params==null){addLog("Outliers removal cancelled", 0);return;}
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		exec.submit(new Runnable() {
			public void run()  {	
				lock();
				tmp.replaceMapsOutliersSlicePerSlice(params[0]<1.5 ? 1 : 2 ,params[1],(int)Math.round(params[2]),true);
		   		hypermapList.add(tmp);		
		   		currentHypermap=tmp;
		   		step++;
		   		updateView();
		   		unlock();
	   		}
		});
  		
	}

	public void runActionAbort() {
		this.addLog("Abort.", 0);
		VitiDialogs.notYet("runActionAbort");
	}
	
	public void runActionUndo() {
		if(hypermapList.size()<2)return;
		this.imgShowedList.get(imgShowedList.size()-1).hide();
		this.currentHypermap=hypermapList.get(hypermapList.size()-2);
		this.hypermapList.remove(hypermapList.size()-1);
		this.imgShowedList.remove(imgShowedList.size()-1);
		this.addLog("Undo.", 0);
		step--;
	}

	public void runActionExplorer() {
		if(this.currentHypermap==null)IJ.showMessage("No Hypermap opened");
		else {
			this.addLog("Starting explorer...", 1);
			new MRI_HyperCurvesExplorer().runExplorerFromHyperMap(this.currentHypermap);
			this.addLog("Explorer is running", 1);
		}
	}
		
	public void runActionStress(){
		VitimageUtils.getRelaxingPopup("Feeling stressed with FijiRelax ?",true);
	}
	
	public void runActionSos(){
		String textToDisplay="<html>"+saut;
		String basicFijiText="<b> </b>FijiRelax is a tool for.....,"+
				" (texte 2 ........) ";
		String mainWindowText=basicFijiText+
				"Texte3 ........ :"+startPar+
				" <b>1)   Texte4........ "+nextPar+
				" <b>2-a) Texte5 ........ ."+nextPar+
				"automatic algorithms settings can be modified using the settings dialog ( <b>\"Advanced settings\"</b> button )."+saut+saut; 

		
	
		disable(SOS);
		textToDisplay+="<b>Citing this work :</b> R. Fernandez and C. Moisy, <i>FijiRelax : fast and noise-corrected estimation of NMR relaxation maps in 3D+t</i> (under review)"+saut+
				"<b>Credits :</b> this work was supported by the \"Plan deperissement du vignoble\" and \"Aplim\" flagship project  </p>";
		IJ.showMessage("FijiRelax help", textToDisplay);
		enable(SOS);
	}
	
	public void runActionOpen() {
		ImagePlus imgNew=VitiDialogs.chooseOneImageUI("Hypermap selection", "Choose nifti or tif hypermap");
		if(imgNew!=null) {
			this.currentHypermap=new HyperMap(imgNew);
			this.hypermapList.add(this.currentHypermap);
		}
		this.addLog("Open Hypermap.", 1);
		this.addLog(VitimageUtils.imageResume(imgNew), 0);
   		step=0;
		updateView();
		enable(new int[] {REGISTER,COMPUTE,OUTLIERS,ABORT,UNDO,EXPLORER,SOS,STRESS} );
		if(this.currentHypermap.T>1)disable(REGISTER);
	}
	
	public void startFromTestImage(String testPath) {
 		ImagePlus imgNew=IJ.openImage(testPath);
		if(imgNew!=null) {
			this.currentHypermap=new HyperMap(imgNew);
			this.hypermapList.add(this.currentHypermap);
		}
		this.addLog("Start from test image :", 0);
		this.addLog(testPath, 0);
  		step=0;
		updateView();
		enable(new int[] {REGISTER,COMPUTE,OUTLIERS,ABORT,UNDO,EXPLORER,SOS,STRESS} );
	}


	
	
	
	
	/** Helpers --------------------------------------------------------------------------------*/	
	public int getRelativeOptimalPositionFor2DView() {
		java.awt.Dimension currentScreen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenX=(int)Math.round(currentScreen.width);
        if(screenX>1920)screenX/=2;        
		if(registrationFrame.getLocationOnScreen().x+registrationFrame.getSize().getWidth()/2 > screenX/2) return 0;
		else return 2;
	}

	
	public RegistrationAction openRegistrationSettingsDialog() {
		RegistrationAction regAct=this.currentHypermap.getDefaultRegistrationSettings();
 		
		//Parameters for automatic registration
		String message="Successive subsampling factors used, from max to min."+"These parameters have the most dramatic effect\non computation time and results accuracy :\n"+
				"- The max level is the first level being processed, making comparisons between subsampled versions of images."+"\n"+
				"  After subsampling images, the algorithm only sees the global structures, but allows transformations of greater amplitude\n"+
				"  High subsampling levels run faster, and low subsampling levels run slower. But if the max subsampling level is too high, the subsampled image\n  is not informative anymore, and registration could diverge"+"\n.\n"+
				"- The min level is the last subsampling level and the more accurate to be processed\n  Subsampling=1 means using all image informations (no subsampling) \n";

		GenericDialog gd= new GenericDialog("Registration settings for Blockmatching of successive echoes");
		String[]levelsMax=new String[4];
		String[]schemes=new String[] {"Rigid","Deformations","Rigid, then deformations"};
		int indCurMin=0;
		int indCurMax=0;
		int indCurScheme=0;
        for(int i=0;i<levelsMax.length;i++) {
        	levelsMax[i]=""+((int)Math.round(Math.pow(2, (i))))+"";
        	if((int)Math.round(Math.pow(2, (i)))==regAct.levelMaxLinear)indCurMax=i;
        	if((int)Math.round(Math.pow(2, (i)))==regAct.levelMinLinear)indCurMin=i;
        }
        gd.addMessage("Registration scheme and target resolutions");
        gd.addChoice("Registration scheme",schemes, schemes[indCurScheme]);
        gd.addChoice("Max subsampling factor (high=fast)",levelsMax, levelsMax[indCurMax]);
        gd.addChoice("Min subsampling factor (low=slow)",levelsMax, levelsMax[indCurMin]);
        gd.addChoice("Higher accuracy (subpixellic level)", new String[] {"Yes","No"},regAct.higherAcc==1 ? "Yes":"No");


        gd.addMessage("Blocks dimensions (image subparts to compare in ref and mov)");
        gd.addNumericField("Block half-size along X", regAct.bhsX, 0, 3, "subsampled pixels");
        gd.addNumericField("... along Y", regAct.bhsY, 0, 3, "subsampled pixels");
        gd.addNumericField("... along Z", regAct.bhsZ, 0, 3, "subsampled pixels");

        gd.addMessage("Maximal distance between matching points (at each iteration)");
        gd.addNumericField("Block neighbourhood along X", regAct.neighX, 0, 3, "subsampled pixels");
        gd.addNumericField("... along Y", regAct.neighY, 0, 3, "subsampled pixels");
        gd.addNumericField("... along Z",  regAct.neighZ, 0, 3, "subsampled pixels");

        gd.addMessage("Spacing between successive blocks along each dimension");
        gd.addNumericField("Striding along X", regAct.strideX, 0, 3, "subsampled pixels");
        gd.addNumericField("... along Y",  regAct.strideY, 0, 3, "subsampled pixels");
        gd.addNumericField("... along Z",  regAct.strideZ, 0, 3, "subsampled pixels");

        gd.addMessage("Others");
        gd.addNumericField("Number of iterations per level (rigid)",  regAct.getIterationsBMLinear(), 0, 3, "iterations");
        gd.addNumericField("Number of iterations per level (deformations)", regAct.getIterationsBMNonLinear(), 0, 3, "iterations");
        gd.addNumericField("Sigma for dense field smoothing", regAct.sigmaDense, 3, 12, "image units");
        gd.addNumericField("Percentage of blocks selected by score", regAct.selectScore, 0, 3, "%");
        gd.addNumericField("Percentage kept in rigid Least-trimmed square", regAct.selectLTS, 0, 3, "%");	        
        if(this.isSurvivorVncTunnelLittleDisplay)gd.setFont(mySurvivalFontForLittleDisplays);
        gd.showDialog();
        if (gd.wasCanceled()) return null;	        
        int scheme=gd.getNextChoiceIndex(); 
        int a=gd.getNextChoiceIndex()+1; regAct.setLevelMaxLinear(a);regAct.setLevelMaxNonLinear(a);
        int b=gd.getNextChoiceIndex()+1; 

        b=(b<a ? b : a); regAct.setLevelMinLinear(b);regAct.setLevelMinNonLinear(b);
        regAct.higherAcc=1-gd.getNextChoiceIndex();
  
     	int c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regAct.bhsX=Math.min(7,Math.max(c,3));
       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regAct.bhsY=Math.min(7,Math.max(c,3));
       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regAct.bhsZ=Math.min(7,Math.max(c,0));

       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regAct.neighX=Math.min(7,Math.max(c,1));
       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regAct.neighY=Math.min(7,Math.max(c,1));
       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regAct.neighZ=Math.min(7,Math.max(c,0));

       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regAct.strideX=Math.min(100,Math.max(c,1));
       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regAct.strideY=Math.min(100,Math.max(c,1));
       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regAct.strideZ=Math.min(100,Math.max(c,1));

       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regAct.setIterationsBMLinear(Math.min(100,Math.max(c,1)));
       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regAct.setIterationsBMNonLinear(Math.min(100,Math.max(c,1)));
        double d=gd.getNextNumber(); d=d<1E-6 ? 1E-6 : d; regAct.sigmaDense=d;
       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regAct.selectScore=Math.min(100,Math.max(c,5));
       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regAct.selectLTS=Math.min(100,Math.max(c,5));
       	if(scheme==0)regAct.setIterationsBMNonLinear(0);
       	else if(scheme==1)regAct.setIterationsBMLinear(0);
       	return regAct;
	}

	public double[] openOutliersDialog() {
		double[]params=new double[3];
		//Parameters for automatic registration
		String message="Successive subsampling factors used, from max to min."+"These parameters have the most dramatic effect\non computation time and results accuracy :\n"+
				"- The max level is the first level being processed, making comparisons between subsampled versions of images."+"\n"+
				"  After subsampling images, the algorithm only sees the global structures, but allows transformations of greater amplitude\n"+
				"  High subsampling levels run faster, and low subsampling levels run slower. But if the max subsampling level is too high, the subsampled image\n  is not informative anymore, and registration could diverge"+"\n.\n"+
				"- The min level is the last subsampling level and the more accurate to be processed\n  Subsampling=1 means using all image informations (no subsampling) \n";
	
		GenericDialog gd= new GenericDialog("Settings for outliers detection and removal");
		String[]algo=new String[2];
		String[]schemes=new String[] {"Tukey fences","MADe"};
 
		gd.addChoice("Removal detection algorithm",schemes, schemes[HyperMap.defaultOutlierAlgorithm-1]);
        gd.addNumericField("Nb standard-dev", HyperMap.defaultOutlierStdDev, 0, 3, "sigma");
        gd.addNumericField("Neighbourhood radius", HyperMap.defaultOutlierNeighbourXY, 0, 3, "pixels");
        if(this.isSurvivorVncTunnelLittleDisplay)gd.setFont(mySurvivalFontForLittleDisplays);
        gd.showDialog();

        if (gd.wasCanceled()) return null;	        
        params[0]=gd.getNextChoiceIndex()+1;
        params[1]=gd.getNextNumber();
        params[2]=gd.getNextNumber();
        if(params[1]<0)params[1]=HyperMap.defaultOutlierStdDev;
        if(params[2]<0)params[2]=HyperMap.defaultOutlierNeighbourXY;
       	return params;
	}
	
	public void updateView() {
		this.addLog("Updating view", 0);
		if(this.hypermapList.size()==0)return;
		ImagePlus temp=null;
		boolean firstView=false;
		if(this.imgView==null || (!this.imgView.isVisible()))firstView=true;
		ImagePlus previousView=null;
		if(!firstView) {
			this.viewSlice=this.imgView.getZ();
			previousView=this.imgView;
		}
		else this.viewSlice=(this.hypermapList.get(this.hypermapList.size()-1).getAsImagePlus().getNSlices())/2;
		
		this.imgView=this.hypermapList.get(this.hypermapList.size()-1).getAsImagePlus();
		if(firstView)this.viewSlice = this.imgView.getNSlices()/2;

		imgView.show();
		imgView.setPosition(1,this.viewSlice,1);
		this.imgShowedList.add(imgView);
		imgView.setTitle("HyperMap step "+step);
		VitimageUtils.adjustFrameOnScreenRelative(imgView.getWindow(),registrationFrame,getRelativeOptimalPositionFor2DView(),0,10);
		java.awt.Rectangle w = imgView.getWindow().getBounds();
		int max=0;
				
		//If little image, enlarge it until its size is between half screen and full screen
		while(imgView.getWindow().getWidth()<(screenWidth/3) && imgView.getWindow().getHeight()<(screenHeight/3) && (max++)<4) {
			int sx=imgView.getCanvas().screenX((int) (w.x+w.width));
			int sy=imgView.getCanvas().screenY((int) (w.y+w.height));
			imgView.getCanvas().zoomIn(sx, sy);
			VitimageUtils.waitFor(50);
			this.lastViewSizes=new int[] {imgView.getWindow().getWidth(),imgView.getWindow().getHeight()};
		}

		//If big image, reduce it until its size is between half screen and full screen
		while(imgView.getWindow().getWidth()>(screenWidth) || imgView.getWindow().getHeight()>(screenHeight)) {
			System.out.println("View : "+(imgView.getWindow().getWidth() +" X "+(imgView.getWindow().getWidth())) );
			int sx=imgView.getCanvas().screenX((int) (w.x+w.width));
			int sy=imgView.getCanvas().screenY((int) (w.y+w.height));
			System.out.println(sx+" "+sy);
			imgView.getCanvas().zoomOut(sx, sy);
			VitimageUtils.waitFor(50);
			this.lastViewSizes=new int[] {imgView.getWindow().getWidth(),imgView.getWindow().getHeight()};
		}
		VitimageUtils.adjustFrameOnScreenRelative(imgView.getWindow(),(firstView ? registrationFrame : previousView.getWindow()),firstView ? 0 : 1,firstView ? 0 : 1,10);
		imgView.updateAndRepaintWindow();
	}
	
	public static String getImgViewText(int st){
		return ( (st==0) ? "Superimposition before registration" :  ("Registration results after "+(st)+" step"+((st>1)? "s" : "")) );
	}
	
	public void closeAllViews() {
		for(ImagePlus img : imgShowedList) {if(img!=null)img.close();};
	}	

	public void welcomeAndInformAboutComputerCapabilities() {		
		String[]str=checkComputerCapacity(true);
		addLog(str[0],0);
		addLog(str[1],0);		
	}
	
	public void addLog(String t,int level) {
		logArea.append((level==0 ? "\n > ": (level==1) ? "\n\n > " : " ")+t);
		logArea.setCaretPosition(logArea.getDocument().getLength());
	}

	@SuppressWarnings("restriction")
	public String []checkComputerCapacity(boolean verbose) {
		this.nbCpu=Runtime.getRuntime().availableProcessors();
		this.jvmMemory=(int)((new Memory().maxMemory() /(1024*1024)));//Java virtual machine available memory (in Megabytes)
		this.memoryFullSize=0;
		String []str=new String[] {"",""};

		str[0]="Welcome to FijiRelax "+versionFlag+" ! \nFirst trial ? Click on \"Contextual help\" to get started. \n User requests, issues ? Contact : romain.fernandezATcirad.fr\n";
		str[1]="System check. Available memory in JVM="+this.jvmMemory+" MB. #Available processor cores="+this.nbCpu+".";
		try {
			this.memoryFullSize =  (( ((com.sun.management.OperatingSystemMXBean) ManagementFactory
		        .getOperatingSystemMXBean()).getTotalPhysicalMemorySize() )/(1024*1024));
		}	catch(Exception e) {return str;}		
		if((this.memoryFullSize>this.jvmMemory*2) && (this.memoryFullSize-this.jvmMemory>4000))  {
			str[1]+="\nIt seems that your computer have more memory : total memory="+(this.memoryFullSize)+" MB.\n"+
					"Registration and map computation are time and memory consuming. To give more memory and computation power to Fiji,"+
					" close the plugin then use the Fiji menu \"Edit / Options / Memory & threads\". "+
					"Let at least "+VitimageUtils.getSystemNeededMemory()+" unused to keep your "+VitimageUtils.getSystemName()+" stable and responsive.";
		}
		if(verbose)		return str;
		else return new String[] {"",""};
	}
	
	
	
	
	
	
	
	
	
	/** Minor helpers --------------------------------------------------------------------------------*/	
	public void lock() {
		this.lock=true;
	}

	public void unlock() {
		this.lock=false;
	}

	public void enable(int but) {
		setState(new int[] {but},true);
	}
	public void disable(int but) {
		setState(new int[] {but},false);
	}

	public void enable(int[]tabBut) {
		setState(tabBut,true);
	}
	public void disable(int[]tabBut) {
		setState(tabBut,false);
	}
			
	public void setState(int[]tabBut,boolean state) {
		for(int but:tabBut) {
			switch(but) {
			case OPEN:this.openButton.setEnabled(state);break;
			case IMPORT:this.importButton.setEnabled(state);break;
			case REGISTER:this.registerButton.setEnabled(state);break;
			case COMPUTE:this.computeButton.setEnabled(state);break;
			case ABORT:this.abortButton.setEnabled(state);break;
			case UNDO:this.undoButton.setEnabled(state);break;
			case EXPLORER:this.explorerButton.setEnabled(state);break;
			case SOS:this.sosButton.setEnabled(state);break;
			case STRESS:this.stressButton.setEnabled(state);break;
			case OUTLIERS:this.outliersButton.setEnabled(state);break;
			}	
		}	
	}


	
}
