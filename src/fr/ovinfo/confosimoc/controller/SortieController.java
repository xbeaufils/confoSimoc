package fr.ovinfo.confosimoc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.ovinfo.confosimoc.ConfoSimoc;
import fr.ovinfo.confosimoc.Resultat;

@RestController()
@RequestMapping("/api/sortie")
public class SortieController {
	@Autowired
	ConfoSimoc simoc;
	
	@PostMapping(value="/verify")
	public Resultat verify(@RequestBody String filename) {
		//ConfoSimoc simoc = new ConfoSimoc();
		return simoc.valideFile(filename);
	}
}
