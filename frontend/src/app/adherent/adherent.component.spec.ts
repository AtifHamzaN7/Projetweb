import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { AdherentComponent } from './adherent.component';

describe('AdherentComponent', () => {
  let component: AdherentComponent;
  let fixture: ComponentFixture<AdherentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdherentComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdherentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});