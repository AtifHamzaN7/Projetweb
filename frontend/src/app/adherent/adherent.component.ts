import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { AdherentService } from '../services/adherent.service';
import { RecetteService, Recette } from '../services/recipe.service';
import { EventService, Event } from '../services/event.service';

@Component({
  selector: 'app-adherent',
  standalone: true,
  templateUrl: './adherent.component.html',
  styleUrls: ['./adherent.component.css'],
  imports: [CommonModule, FormsModule, RouterModule],
  providers: [AdherentService]
})
export class AdherentComponent implements OnInit {
  nom: string = '';
  prenom: string = '';
  email: string = '';
  showDialog: boolean = false;

  recettesAdherent: Recette[] = [];

  evenementsOrganises: Event[] = [];
  evenementsParticipes: Event[] = [];

  constructor(
    private router: Router,
    private adherentService: AdherentService,
    private recetteService: RecetteService,
    private eventService: EventService
  ) {}

    // Add this method
  getImageUrl(filename: string): string {
    if (!filename) return '';
    return `http://localhost:8080/images/${filename}`;
  }
  
  ngOnInit(): void {
    const adherentStr = localStorage.getItem('adherent');
    if (!adherentStr) {
      console.warn('Aucun adhérent connecté trouvé.');
      this.router.navigate(['/conn']);
      return;
    }

    const adherent = JSON.parse(adherentStr);
    const idAdh = adherent.idAdh;
    this.nom = adherent.nom;
    this.prenom = adherent.prenom;
    this.email = adherent.email;

    if (!idAdh) {
      console.error('ID de l’adhérent invalide.');
      this.router.navigate(['/conn']);
      return;
    }

    this.recetteService.getRecettes().subscribe((recettes: Recette[]) => {
      this.recettesAdherent = recettes.filter(r => r.auteur?.idAdh === idAdh);
    });

    // 🎯 Récupération des événements organisés
    this.eventService.getEvenementsParAdherent(idAdh).subscribe({
      next: (data) => {
        this.evenementsOrganises = data;
      },
      error: (err) => {
        console.error("Erreur chargement événements organisés :", err);
      }
    });

    // 👥 Récupération des événements participés (hors organisés)
    this.eventService.getEvenementsParParticipant(idAdh).subscribe({
      next: (data) => {
        this.evenementsParticipes = data.filter(e => e.auteur.email !== this.email);
      },
      error: (err) => {
        console.error("Erreur chargement participations :", err);
      }
    });
  }

  openDialog(): void {
    this.showDialog = true;
  }

  closeDialog(): void {
    this.showDialog = false;
  }

  logout(): void {
    localStorage.removeItem('adherent');
    this.router.navigate(['/conn']);
  }

  goToAddRecipe() {
    this.router.navigate(['/add-recipe']);
  }
}
