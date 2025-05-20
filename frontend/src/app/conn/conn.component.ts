import { Component } from '@angular/core';
import { NgForm } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdherentService, Adherent } from '../services/adherent.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-conn',
  standalone: true,
  templateUrl: './conn.component.html',
  styleUrls: ['./conn.component.css'],
  imports: [FormsModule, CommonModule],
})
export class ConnComponent {
  constructor(
    private adherentService: AdherentService,
    private router: Router
  ) {}

  onSubmit(form: NgForm) {
    if (form.valid) {
      const { email, password } = form.value;

      this.adherentService.getByEmailAndPassword(email, password).subscribe({
        next: (adherent: Adherent) => {
          console.log('✅ Connexion réussie :', adherent);

          console.log('nom =', adherent.nom);
          console.log('email =', adherent.email);
          // Enregistrer l'utilisateur connecté si besoin :
          localStorage.setItem('adherent', JSON.stringify(adherent));


          // Redirection vers la page adhérent
          this.router.navigate(['/adherent']);
        },
        error: (err) => {
          console.error('❌ Erreur de connexion', err);
          alert('Email ou mot de passe incorrect');
        }
      });
    }
  }
}
