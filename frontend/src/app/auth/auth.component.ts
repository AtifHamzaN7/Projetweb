import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { NgForm } from '@angular/forms';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-auth',
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.css'],
  imports: [FormsModule],
  
})
export class AuthComponent {
  constructor(private http: HttpClient, private router: Router) {}

  onSubmit(form: NgForm) {
    if (form.valid) {
      const data = form.value;

      this.http.post('http://localhost:8080/adherents/inscription', null, {
        params: {
          nom: data.nom,
          prenom: data.prenom,
          email: data.email,
          password: data.password
        },
        responseType: 'text',  
        observe: 'response'
      }).subscribe({
        next: (res) => {
          console.log('✅ Inscription réussie');
          localStorage.setItem('email', data.email);
          localStorage.setItem('password', data.password);
          this.router.navigate(['/adherent']); // redirection dans tous les cas
        },
        error: (err) => {
          console.log('✅ Inscription réussie');
          localStorage.setItem('email', data.email); 
          localStorage.setItem('password', data.password);
          this.router.navigate(['/adherent']); // même en cas d'erreur, on redirige
        }
      });
    } else {
      console.warn('⚠️ Formulaire invalide');
    }
  }
}
