import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AdherentService, Adherent } from '../services/adherent.service'; // adapte le chemin si nécessaire
import { Router } from '@angular/router';

@Component({
  selector: 'app-adherent',
  standalone: true,
  templateUrl: './adherent.component.html',
  styleUrls: ['./adherent.component.css'],
  imports: [CommonModule, FormsModule],
  providers: [AdherentService] 
})
export class AdherentComponent implements OnInit {
  nom: string = '';
  prenom: string = '';
  email: string = '';

  showDialog: boolean = false;

  constructor(private router: Router,private adherentService: AdherentService) {}

  ngOnInit(): void {
    const adherentStr = localStorage.getItem('adherentConnecte');
  if (adherentStr) {
    const adherent = JSON.parse(adherentStr);
    this.nom = adherent.nom;
    this.prenom = adherent.prenom;
    this.email = adherent.email;
  } else {
    console.warn('Aucun adhérent connecté trouvé.');
    this.router.navigate(['/conn']);
  }
}

  openDialog(): void {
    this.showDialog = true;
  }

  closeDialog(): void {
    this.showDialog = false;
  }
  
  logout(): void {
  localStorage.removeItem('adherentConnecte');
  this.router.navigate(['/conn']);
}
}
