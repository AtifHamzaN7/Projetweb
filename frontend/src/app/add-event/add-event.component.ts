import { Component } from '@angular/core';
import { EventService } from '../services/event.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'app-add-event',
  templateUrl: './add-event.component.html',
  styleUrls: ['./add-event.component.css'],
  imports: [CommonModule, FormsModule],
})
export class AddEventComponent {
  titre = '';
  description = '';
  date = '';
  lieu = '';

  constructor(private eventService: EventService, private router: Router) {}

  onSubmit(form: NgForm) {
  if (form.valid) {
    const data = form.value;

    const adherentStr = localStorage.getItem('adherent');
    if (!adherentStr) {
      alert("Vous devez être connecté.");
      return;
    }

    const adherent = JSON.parse(adherentStr);
    const auteurId = adherent.idAdh;

    this.eventService.createEvent(data, auteurId).subscribe({
      next: () => {
        console.log("✅ Événement créé");
        this.router.navigate(['/evenements']);
      },
      error: (err) => {
        console.error("❌ Erreur création événement", err);
        alert("Erreur lors de la création.");
      }
    });
  }
}
  }
