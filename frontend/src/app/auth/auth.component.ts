import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule], 
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.css']
})
export class AuthComponent {
  isLoginMode = true;

  toggleMode() {
    this.isLoginMode = !this.isLoginMode;
  }

  onSubmit(formData: any) {
    console.log(this.isLoginMode ? 'Logging in' : 'Signing up', formData);
  }
}
