import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../shared/shared.module';



@Component({
  selector: 'app-home',
  imports: [ CommonModule, SharedModule,  RouterModule],
  standalone: true,
  // The templateUrl and styleUrls properties are used to specify the HTML and CSS files for the component.
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {

}
