import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from './navbar/navbar.component';
import { FooterComponent } from './footer/footer.component';
import { CardComponent } from './card/card.component';
import { PageHeaderComponent } from './page-header/page-header.component';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    NavbarComponent,
    FooterComponent,
    CardComponent,
    PageHeaderComponent
  ],
  exports: [
    NavbarComponent,
    FooterComponent,
    CardComponent,
    PageHeaderComponent
  ]
})
export class SharedModule { }