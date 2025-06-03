import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ForumService, Message } from '../../services/forum.service';

@Component({
  selector: 'app-mes-messages',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './mes-messages.component.html',
  styleUrls: ['./mes-messages.component.css']
})
export class MesMessagesComponent implements OnInit {
  messages: Message[] = [];

  constructor(private forumService: ForumService) {}

  ngOnInit(): void {
    const adherentStr = localStorage.getItem('adherent');
    if (!adherentStr) {
      console.warn("Aucun adhérent connecté.");
      return;
    }

    const adherent = JSON.parse(adherentStr);
    const idAdh = adherent.idAdh;

    if (!idAdh) {
      console.error("ID adhérent invalide.");
      return;
    }

    this.forumService.getMessagesByAdherent(idAdh).subscribe({
      next: (msgs) => {
        this.messages = msgs;
        console.log("✅ Messages récupérés :", msgs);
      },
      error: (err) => {
        console.error("❌ Erreur chargement messages :", err);
      }
    });
  }
}
