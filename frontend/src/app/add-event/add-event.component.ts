// add-event.component.ts
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { EventService } from '../services/event.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-add-event',
  styleUrls: ['./add-event.component.css'],

  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-event.component.html',
})
export class AddEventComponent {
  eventForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private eventService: EventService
  ) {
    this.eventForm = this.fb.group({
      titre: ['', Validators.required],
      description: ['', Validators.required],
      date: ['', Validators.required],
      lieu: ['', Validators.required]
    });
  }

  onSubmit(): void {
    const adherent = JSON.parse(localStorage.getItem('adherent')!);
    const auteurId = adherent.idAdh;

    if (this.eventForm.valid) {
      this.eventService.createEvent(this.eventForm.value, auteurId).subscribe({
        next: () => {
          alert('✅ Événement créé avec succès !');
          this.router.navigate(['/adherent']);
        },
        error: (err) => {
          console.error('❌ Erreur lors de la création :', err);
          alert('Erreur lors de la création de l’événement.');
        }
      });
    }
  }
}
