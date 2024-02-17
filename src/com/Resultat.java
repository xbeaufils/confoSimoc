package com;


import java.util.List;

import fr.cmre.simoc.xmlObject.V2.Anomalie;
import lombok.Data;

@Data
public class Resultat {
    List<String> erreurs; 
    List<String> anomalies; 
    List<Anomalie> xmlAnomalies;
}
