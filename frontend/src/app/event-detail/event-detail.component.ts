import { Component, OnInit } from '@angular/core';
import { ActivatedRoute} from '@angular/router';
import { EventService, Event } from '../services/event.service';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';


@Component({
  selector: 'app-event-detail',
  standalone: true,
  templateUrl: './event-detail.component.html',
  styleUrls: ['./event-detail.component.css'],
  imports: [CommonModule, RouterModule],
})
export class EventDetailComponent implements OnInit {
  event: Event | undefined;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private eventService: EventService
  ) {}
  
  retour(): void {
  this.router.navigate(['/evenements']);
}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!isNaN(id)) {
      this.eventService.getEventById(id).subscribe({
        next: (data) => {
          this.event = data;
        },
        error: (err) => {
          console.error('❌ Erreur de chargement de l’événement :', err);
        }
      });
    } else {
      console.warn('⚠️ ID d’événement invalide dans l’URL');
    }
  }

  participer(): void {
    const adherentStr = localStorage.getItem('adherent');
    if (!adherentStr) {
      alert("Veuillez vous connecter pour participer.");
      this.router.navigate(['/auth']);
      return;
    }

    const adherent = JSON.parse(adherentStr);
    const idAdh = adherent.idAdh;

    if (!this.event) return;

    this.eventService.participerAEvent(this.event.id, idAdh).subscribe({
      next: () => {
        alert("✅ Participation confirmée !");
       this.router.navigate(['/adherent']);
; // Recharger l'événement pour mettre à jour les participants
      },
      error: (err) => {
        console.error("❌ Erreur lors de la participation :", err);
        alert("Erreur lors de la participation.");
      }
    });
  }
}
