import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ForumService, Discussion, Message } from '../../services/forum.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';


@Component({
  selector: 'app-discussion-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './discussion-detail.component.html',
  styleUrls: ['./discussion-detail.component.css']
})
export class DiscussionDetailComponent implements OnInit {
  discussion!: Discussion;
  messages: Message[] = [];
  newMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private forumService: ForumService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) return;

    this.forumService.getAllDiscussions().subscribe((discs) => {
      const found = discs.find(d => d.idDisc === id);
      if (found) this.discussion = found;
    });

    this.forumService.getMessagesByDiscussion(id).subscribe((msgs) => {
      this.messages = msgs;
    });
  }

  envoyer(): void {
  if (!this.newMessage.trim()) return;

  const adherentStr = localStorage.getItem('adherent');
  if (!adherentStr) {
    alert("Veuillez vous connecter pour répondre.");
    return;
  }

  const adherent = JSON.parse(adherentStr);
  this.forumService
    .postMessage(this.discussion.idDisc, adherent.idAdh, this.newMessage)
    .subscribe({
      next: () => {
        alert("✅ Message envoyé !");
        this.router.navigate(['/adherent']);
      },
      error: (err) => {
        console.error("❌ Erreur lors de l'envoi du message :", err);
        alert("Erreur lors de l'envoi.");
      }
    });
}

}
