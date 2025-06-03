import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ForumService, Message } from '../../services/forum.service';

@Component({
  selector: 'app-mes-messages',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mes-messages.component.html',
  styleUrls: ['./mes-messages.component.css']
})
export class MesMessagesComponent implements OnInit {
  messages: Message[] = [];

  constructor(private forumService: ForumService) {}

  ngOnInit(): void {
    const adherentStr = localStorage.getItem('adherent');
    if (!adherentStr) return;

    const adherent = JSON.parse(adherentStr);
    const idAdh = adherent.idAdh;

    this.forumService.getMessagesByAdherent(idAdh).subscribe({
      next: (msgs) => {
        this.messages = msgs;
      },
      error: (err) => {
        console.error('Erreur chargement messages de l’adhérent :', err);
      }
    });
  }
}
