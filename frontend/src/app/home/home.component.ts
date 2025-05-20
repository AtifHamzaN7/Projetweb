import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../shared/shared.module';
import { EventsComponent } from "../events/events.component";



@Component({
  selector: 'app-home',
  imports: [CommonModule, SharedModule, RouterModule, EventsComponent],
  standalone: true,
  // The templateUrl and styleUrls properties are used to specify the HTML and CSS files for the component.
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {

}
