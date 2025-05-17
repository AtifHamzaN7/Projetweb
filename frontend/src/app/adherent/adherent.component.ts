import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdherentService } from '../services/adherent.service';
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

  constructor(
    private router: Router,
    private adherentService: AdherentService,
    private recetteService: RecetteService
  ) {}

  ngOnInit(): void {
    // ✅ On lit l’adhérent connecté depuis le localStorage
    const adherentStr = localStorage.getItem('adherent');
    if (!adherentStr) {
      console.warn('Aucun adhérent connecté trouvé.');
      this.router.navigate(['/conn']);
      return;
    }

    const adherent = JSON.parse(adherentStr);
    const idAdh = adherent.idAdh;

    if (!idAdh) {
      console.error('ID de l’adhérent invalide.');
      this.router.navigate(['/conn']);
      return;
    }

    this.nom = adherent.nom;
    this.prenom = adherent.prenom;
    this.email = adherent.email;

    console.log('ID adhérent connecté :', idAdh);

    this.recetteService.getRecettes().subscribe((recettes: Recette[]) => {
      console.log('Toutes les recettes :', recettes);
      this.recettesAdherent = recettes.filter(r => r.auteur?.idAdh === idAdh);
      console.log('Recettes de cet adhérent :', this.recettesAdherent);
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
