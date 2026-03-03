import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { AjoutDiscussionComponent } from './ajout-discussion.component';

describe('AjoutDiscussionComponent', () => {
  let component: AjoutDiscussionComponent;
  let fixture: ComponentFixture<AjoutDiscussionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AjoutDiscussionComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AjoutDiscussionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
