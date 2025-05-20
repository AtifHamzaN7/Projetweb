import { Component, OnInit } from '@angular/core';
import { EventService, Event } from '../services/event.service';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';



@Component({
  selector: 'app-events',
  templateUrl: './events.component.html',
  styleUrls: ['./events.component.css'],
  imports: [CommonModule, RouterModule],
})
export class EventsComponent implements OnInit {
  evenements: Event[] = [];

  constructor(private eventService: EventService) {}

  ngOnInit(): void {
    this.eventService.getEvenements().subscribe(data => {
      this.evenements = data;
    });
  }
}
