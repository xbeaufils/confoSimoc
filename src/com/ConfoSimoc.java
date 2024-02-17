package com;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.teamlog.simoc.xml.Controle;
import com.teamlog.simoc.xml.MsgErr;
import com.teamlog.simoc.xml.XmlUtils;
import com.teamlog.simoc.xmlObject.NotificationMouvementLotOvinCaprin;

import fr.cmre.simoc.xml.V2.ControleV2;
import fr.cmre.simoc.xmlObject.V2.Anomalie;

@Service
public class ConfoSimoc {
	   static final String FICHIER_XSD_V111 = "XMLNotificationOvinCaprinV111.XSD";
	    static final String FICHIER_XSD_V2 = "XMLNotificationOvinCaprinV2.XSD";
	    private static final String FILE_NAME = "confosimoc";
	    static final String REP_TRAVAIL_DEFAUT = "validation";
	    static final String REP_XSD_V111 = "xsd";
	    static final String REP_XSD_V2 = "xsd_V2";
	    private static ResourceBundle bundle;
	    private static Boolean dateV2depassee = null;
	    private static File fichierXsd = null;
	    private static File fichierXsdV111 = null;
	    private static File fichierXsdV2 = null;
	    private static Log logger = LogFactory.getLog(ConfoSimoc.class);
	    private static File repTravail = null;
	    private static String versionSchemaReference = null;
		@Value("${xml.repository}")
		private String xmlRepository;

	    static {
	        bundle = null;
	        try {
	            bundle = ResourceBundle.getBundle(FILE_NAME);
	        } catch (Exception e) {
	            e.printStackTrace();
	            System.err.println("Erreur lors de la récupération dans le fichier de config confosimoc");
	        }
	    }
	    
	    public static void main(String[] args) {
	        if (true /*Initialisation(args)*/) {
	            JAXBContext jc = null;
	            try {
	                jc = JAXBContext.newInstance("com.teamlog.simoc.xmlObject");
	            } catch (JAXBException e) {
	            	System.err.println(e.getMessage());
	                System.exit(1);
	            }
	            JAXBContext jcV2 = null;
	            try {
	                jcV2 = JAXBContext.newInstance("fr.cmre.simoc.xmlObject.V2");
	            } catch (JAXBException e2) {
	                System.err.println(e2.getMessage());
	                System.exit(1);
	            }
	            File[] fichiers = repTravail.listFiles();
	            int length = fichiers.length;
	            int i = 0;
	            while (true) {
	                int i2 = i;
	                if (i2 >= length) {
	                    break;
	                }
	                File fichier = fichiers[i2];
	                NotificationMouvementLotOvinCaprin mvtOC = null;
	                fr.cmre.simoc.xmlObject.V2.NotificationMouvementLotOvinCaprin mvtOCV2 = null;
	                List<String> erreurs = new ArrayList<>();
	                List<String> anomalies = new ArrayList<>();
	                boolean fichierValide = false;
	                try {
	                    System.out.println("Traitement du fichier : " + fichier.getName());
	                    System.out.println("[1] Recherche du schéma de référence du fichier xml");
	                    versionSchemaReference = XmlUtils.rechercherSchemaReference(fichier.getPath(), erreurs);
	                    if (versionSchemaReference != null && "V111".equals(versionSchemaReference) && !dateV2depassee.booleanValue()) {
	                        fichierXsd = fichierXsdV111;
	                    } else if (versionSchemaReference == null || !"V2".equals(versionSchemaReference)) {
	                        fichierXsd = null;
	                    } else {
	                        fichierXsd = fichierXsdV2;
	                    }
	                    if (fichierXsd == null) {
	                        erreurs.add(MsgErr.getMsgErr("JG920", versionSchemaReference));
	                        fichierValide = false;
	                    } else if (XmlUtils.formatNomFichierValide(fichier.getName(), ".xml")) {
	                        System.out.println("[2] Contrôle des codes ASCII");
	                        if (XmlUtils.ControlerCodeASCII(fichier.getPath(), erreurs, versionSchemaReference)) {
	                            System.out.println("[3] Conversion du fichier xml");
	                            if ("V111".equals(versionSchemaReference)) {
	                                mvtOC = XmlUtils.lireFichierXML(jc, fichier.getPath(), fichierXsd.getAbsolutePath(), erreurs, versionSchemaReference);
	                            } else if ("V2".equals(versionSchemaReference)) {
	                                mvtOCV2 = XmlUtils.lireFichierXMLV2(jcV2, fichier.getPath(), fichierXsd.getAbsolutePath(), erreurs, versionSchemaReference);
	                            }
	                            System.out.println("[4] Contrôle de cohérence des données");
	                            if (mvtOC != null) {
	                                if (Controle.VerifierCoherence(mvtOC, erreurs, fichier.getName(), versionSchemaReference)) {
	                                    fichierValide = true;
	                                }
	                            }
	                            if (mvtOCV2 != null) {
	                                if (ControleV2.VerifierCoherence(mvtOCV2, erreurs, fichier.getName(), versionSchemaReference, dateV2depassee)) {
	                                    fichierValide = true;
	                                }
	                            }
	                        }
	                    } else {
	                        System.out.println("Rejet du fichier " + fichier.getName() + " : erreur de nommage");
	                        erreurs.add("erreur de nommage");
	                        fichierValide = false;
	                    }
	                } catch (Exception e3) {
	                    System.err.println("Erreur inattendue sur le fichier " + fichier.getName());
	                    System.err.println(e3.toString());
	                    StringBuffer sb = new StringBuffer();
	                    StackTraceElement[] st = e3.getStackTrace();
	                    int length2 = st.length;
	                    for (int i3 = 0; i3 < length2; i3++) {
	                        sb.append(st[i3].toString()).append(System.getProperty("line.separator"));
	                    }
	                    System.err.println(sb.toString());
	                    erreurs.add("Erreur inattendue");
	                }
	                if (!fichierValide) {
	                    String nomSansExtension = fichier.getName().substring(0, fichier.getName().length() - 4);
	                    if (erreurs.size() == 0) {
	                        File file = new File(repTravail.getAbsolutePath(), String.valueOf(nomSansExtension) + "_REP.xml");
	                        System.out.println("Création du fichier de réponse " + file.getPath());
	                        if (mvtOC != null) {
	                            mvtOC.setIdentification(file.getName());
	                            mvtOC.setDocumentOrigine(fichier.getName());
	                            Date date = new Date();
	                            GregorianCalendar gCalendar = new GregorianCalendar();
	                            gCalendar.setTime(date);
	                            try {
	                                mvtOC.setCreation(DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar));
	                            } catch (DatatypeConfigurationException e4) {
	                                System.out.println(e4.getMessage());
	                            }
	                            System.out.println("Reécriture du fichier xml " + file.getName());
	                            XmlUtils.ecrireFichierXML(jc, mvtOC, file.getPath());
	                        }
	                        if (mvtOCV2 != null) {
	                            mvtOCV2.setIdentification(file.getName());
	                            mvtOCV2.setDocumentOrigine(fichier.getName());
	                            Date date2 = new Date();
	                            GregorianCalendar gCalendar2 = new GregorianCalendar();
	                            gCalendar2.setTime(date2);
	                            try {
	                                mvtOCV2.setCreation(DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar2));
	                            } catch (DatatypeConfigurationException e5) {
	                                System.out.println(e5.getMessage());
	                            }
	                            System.out.println("Reécriture du fichier xml " + file.getName());
	                            XmlUtils.ecrireFichierXMLV2(jc, mvtOCV2, file.getPath());
	                        }
	                    } else {
	                        File file2 = new File(repTravail.getAbsolutePath(), String.valueOf(nomSansExtension) + "_ERR.txt");
	                        System.out.println("Ecriture du fichier log " + file2.getPath());
	                        XmlUtils.EcrireFichierLog(file2.getPath(), erreurs);
	                    }
	                }
	                if (versionSchemaReference != null && "V111".equals(versionSchemaReference) && !dateV2depassee.booleanValue()) {
	                    String nomSansExtension2 = fichier.getName().substring(0, fichier.getName().length() - 4);
	                    File file3 = new File(repTravail.getAbsolutePath(), String.valueOf(nomSansExtension2) + "_ANO.txt");
	                    System.out.println("Ecriture du fichier log " + file3.getPath());
	                    anomalies.add(bundle.getString("A001"));
	                    XmlUtils.EcrireFichierLog(file3.getPath(), anomalies);
	                }
	                i = i2 + 1;
	            }
	        } else {
	            System.exit(1);
	        }
	        System.exit(0);
	    }

	    public Resultat valideFile(String filename) {
	        JAXBContext jc = null;
	        JAXBContext jcV2 = null;
	        if (this.Initialisation()) {
	            try {
	                jc = JAXBContext.newInstance("com.teamlog.simoc.xmlObject");
	            } catch (JAXBException e) {
	            	System.err.println(e.getMessage());
	                System.exit(1);
	            }
	            try {
	                jcV2 = JAXBContext.newInstance("fr.cmre.simoc.xmlObject.V2");
	            } catch (JAXBException e2) {
	                System.err.println(e2.getMessage());
	                System.exit(1);
	            }
	        }
	        File fichier = new File(repTravail.getAbsolutePath(),filename) ;
            NotificationMouvementLotOvinCaprin mvtOC = null;
            fr.cmre.simoc.xmlObject.V2.NotificationMouvementLotOvinCaprin mvtOCV2 = null;
            List<String> erreurs = new ArrayList<>();
            List<String> anomalies = new ArrayList<>();
            List<Anomalie> xmlAnomalies = new ArrayList<Anomalie>();
            boolean fichierValide = false;
            try {
                System.out.println("Traitement du fichier : " + fichier.getName());
                System.out.println("[1] Recherche du schéma de référence du fichier xml");
                versionSchemaReference = XmlUtils.rechercherSchemaReference(fichier.getPath(), erreurs);
                if (versionSchemaReference != null && "V111".equals(versionSchemaReference) && !dateV2depassee.booleanValue()) {
                    fichierXsd = fichierXsdV111;
                } else if (versionSchemaReference == null || !"V2".equals(versionSchemaReference)) {
                    fichierXsd = null;
                } else {
                    fichierXsd = fichierXsdV2;
                }
                if (fichierXsd == null) {
                    erreurs.add(MsgErr.getMsgErr("JG920", versionSchemaReference));
                    fichierValide = false;
                } else if (XmlUtils.formatNomFichierValide(fichier.getName(), ".xml")) {
                    System.out.println("[2] Contrôle des codes ASCII");
                    if (XmlUtils.ControlerCodeASCII(fichier.getPath(), erreurs, versionSchemaReference)) {
                        System.out.println("[3] Conversion du fichier xml");
                        if ("V111".equals(versionSchemaReference)) {
                            mvtOC = XmlUtils.lireFichierXML(jc, fichier.getPath(), fichierXsd.getAbsolutePath(), erreurs, versionSchemaReference);
                        } else if ("V2".equals(versionSchemaReference)) {
                            mvtOCV2 = XmlUtils.lireFichierXMLV2(jcV2, fichier.getPath(), fichierXsd.getAbsolutePath(), erreurs, versionSchemaReference);
                        }
                        System.out.println("[4] Contrôle de cohérence des données");
                        if (mvtOC != null) {
                            if (Controle.VerifierCoherence(mvtOC, erreurs, fichier.getName(), versionSchemaReference)) {
                                fichierValide = true;
                            }
                        }
                        if (mvtOCV2 != null) {
                            if (ControleV2.VerifierCoherence(mvtOCV2, erreurs, fichier.getName(), versionSchemaReference, dateV2depassee)) {
                                fichierValide = true;
                            }
                        }
                    }
                } else {
                    System.out.println("Rejet du fichier " + fichier.getName() + " : erreur de nommage");
                    erreurs.add("erreur de nommage");
                    fichierValide = false;
                }
            } catch (Exception e3) {
                System.err.println("Erreur inattendue sur le fichier " + fichier.getName());
                System.err.println(e3.toString());
                StringBuffer sb = new StringBuffer();
                StackTraceElement[] st = e3.getStackTrace();
                int length2 = st.length;
                for (int i3 = 0; i3 < length2; i3++) {
                    sb.append(st[i3].toString()).append(System.getProperty("line.separator"));
                }
                System.err.println(sb.toString());
                erreurs.add("Erreur inattendue");
            }
            if (!fichierValide) {
                String nomSansExtension = fichier.getName().substring(0, fichier.getName().length() - 4);
                if (erreurs.size() == 0) {
                    File file = new File(repTravail.getAbsolutePath(), String.valueOf(nomSansExtension) + "_REP.xml");
                    System.out.println("Création du fichier de réponse " + file.getPath());
                    if (mvtOC != null) {
                        mvtOC.setIdentification(file.getName());
                        mvtOC.setDocumentOrigine(fichier.getName());
                        Date date = new Date();
                        GregorianCalendar gCalendar = new GregorianCalendar();
                        gCalendar.setTime(date);
                        try {
                            mvtOC.setCreation(DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar));
                        } catch (DatatypeConfigurationException e4) {
                            System.out.println(e4.getMessage());
                        }
                        System.out.println("Reécriture du fichier xml " + file.getName());
                        XmlUtils.ecrireFichierXML(jc, mvtOC, file.getPath());
                    }
                    if (mvtOCV2 != null) {
                        mvtOCV2.setIdentification(file.getName());
                        mvtOCV2.setDocumentOrigine(fichier.getName());
                        Date date2 = new Date();
                        GregorianCalendar gCalendar2 = new GregorianCalendar();
                        gCalendar2.setTime(date2);
                        try {
                            mvtOCV2.setCreation(DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar2));
                        } catch (DatatypeConfigurationException e5) {
                            System.out.println(e5.getMessage());
                        }
                        System.out.println("Reécriture du fichier xml " + file.getName());
                        XmlUtils.ecrireFichierXMLV2(jc, mvtOCV2, file.getPath());
                        mvtOCV2.getCirculationOrienteeChargement().get(0).getValidation().getAnomalie().forEach( 
                        		(Anomalie anomalie) -> {
                        			anomalies.add(anomalie.getCodeAnomalie().value() + ":" + anomalie.getLibelleAnomalie() );
                        			xmlAnomalies.add(anomalie);
                        		});
                    } 
                } else {
                    File file2 = new File(repTravail.getAbsolutePath(), String.valueOf(nomSansExtension) + "_ERR.txt");
                    System.out.println("Ecriture du fichier log " + file2.getPath());
                    XmlUtils.EcrireFichierLog(file2.getPath(), erreurs);
                }
            }
            if (versionSchemaReference != null && "V111".equals(versionSchemaReference) && !dateV2depassee.booleanValue()) {
                String nomSansExtension2 = fichier.getName().substring(0, fichier.getName().length() - 4);
                File file3 = new File(repTravail.getAbsolutePath(), String.valueOf(nomSansExtension2) + "_ANO.txt");
                System.out.println("Ecriture du fichier log " + file3.getPath());
                anomalies.add(bundle.getString("A001"));
                XmlUtils.EcrireFichierLog(file3.getPath(), anomalies);
            }
            Resultat result = new Resultat();
            result.setErreurs(erreurs);
            result.setAnomalies(anomalies);
            result.setXmlAnomalies(xmlAnomalies);
            return result;
	    }
	    
	    private boolean Initialisation(/*String[] args*/) {
	        boolean resultat = true;
/*	        if (args.length > 1) {
	            System.err.println("Nombre de paramètres de l'application invalide.");
	            resultat = false;
	        } else if (args.length == 1) {
	            File arg = new File(args[0]);
	            if (!arg.isDirectory() && !arg.isFile()) {
	                System.err.println("Le paramètre [" + args[0] + "] n'est ni un répertoire ni un fichier valide.");
	                resultat = false;
	            } else if (arg.isDirectory()) {
	                repTravail = new File(args[0]);
	            }
	        } else {
	            repTravail = new File(REP_TRAVAIL_DEFAUT);
	        }
*/	        repTravail = new File(this.xmlRepository);
	        fichierXsdV111 = new File(REP_XSD_V111, FICHIER_XSD_V111);
	        if (!fichierXsdV111.exists()) {
	            System.err.println("Le répertoire " + fichierXsdV111.getAbsolutePath() + " n'existe pas.");
	            resultat = false;
	        }
	        fichierXsdV2 = new File(REP_XSD_V2, FICHIER_XSD_V2);
	        if (!fichierXsdV2.exists()) {
	            System.err.println("Le répertoire " + fichierXsdV2.getAbsolutePath() + " n'existe pas.");
	            resultat = false;
	        }
	        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
	        try {
	            if (sdf.parse(sdf.format(new Date())).compareTo(sdf.parse(bundle.getString("DATE_V2"))) >= 0) {
	                dateV2depassee = true;
	                return resultat;
	            }
	            dateV2depassee = false;
	            return resultat;
	        } catch (Exception e) {
	            System.err.println("Erreur lors de la recherche si schéma de référence V111 autorisé");
	            return false;
	        }
	    }

}
