import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ForumService, Discussion } from '../../services/forum.service';
import { RouterModule, Router } from '@angular/router';

@Component({
  selector: 'app-forum',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './forum.component.html',
  styleUrls: ['./forum.component.css']
})
export class ForumComponent implements OnInit {
  discussions: Discussion[] = [];

  constructor(private forumService: ForumService, private router: Router) {}

  ngOnInit(): void {
    this.forumService.getAllDiscussions().subscribe({
      next: (data) => {
        this.discussions = data;
      },
      error: (err) => {
        console.error('Erreur chargement discussions :', err);
      }
    });
  }

  voirDiscussion(id: number): void {
    this.router.navigate(['/forum/discussion', id]);
  }

  ajouterDiscussion(): void {
    this.router.navigate(['/forum/ajouter']);
  }
}
