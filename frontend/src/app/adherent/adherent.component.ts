import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AdherentService, Adherent } from '../services/adherent.service'; // adapte le chemin si nécessaire
import { RecetteService, Recette } from '../services/recipe.service';


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

  recettesAdherent: Recette[] = [];
  adherentId!: number;

  constructor(private adherentService: AdherentService, private recetteService: RecetteService) {}

  ngOnInit(): void {
    const email = localStorage.getItem('email');
    const password = localStorage.getItem('password');

    if (email && password) {
      this.adherentService.getByEmailAndPassword(email, password).subscribe({
        next: (adherent: Adherent) => {
          this.nom = adherent.nom;
          this.prenom = adherent.prenom;
          this.email = adherent.email;
        },
        error: (err) => {
          console.error('❌ Erreur lors du chargement de l’adhérent :', err);
        }
      });
    } else {
      console.warn('⚠️ Aucune information de connexion trouvée dans le localStorage');
    }

    const adherentConnecte = JSON.parse(localStorage.getItem('adherent') || '{}');
    const idAdh = adherentConnecte.idAdh;

    this.recetteService.getRecettes().subscribe((recettes: Recette[]) => {
      this.recettesAdherent = recettes.filter(recette => recette.auteur.idAdh === idAdh);
    });
  }

  openDialog(): void {
    this.showDialog = true;
  }

  closeDialog(): void {
    this.showDialog = false;
  }
}
