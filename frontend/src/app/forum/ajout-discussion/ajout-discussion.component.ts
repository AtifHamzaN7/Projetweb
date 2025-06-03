import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ForumService } from '../../services/forum.service';

@Component({
  selector: 'app-ajout-discussion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ajout-discussion.component.html',
  styleUrls: ['./ajout-discussion.component.css']
})
export class AjoutDiscussionComponent {
  titre: string = '';
  question: string = '';

  constructor(private forumService: ForumService, private router: Router) {}

  onSubmit(): void {let adherentStr: string | null = null;

if (typeof window !== 'undefined') {
  adherentStr = localStorage.getItem('adherent');
}


const adherent = JSON.parse(adherentStr!);
    this.forumService.postDiscussion(this.titre, this.question, adherent.idAdh).subscribe({
      next: () => {
        alert("✅ Discussion créée !");
        this.router.navigate(['/adherent']);

      },
      error: (err) => {
        console.error("❌ Erreur création discussion :", err);
        alert("Erreur lors de la création de la discussion.");
      }
    });
  }
}
