import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AjoutDiscussionComponent } from './ajout-discussion.component';

describe('AjoutDiscussionComponent', () => {
  let component: AjoutDiscussionComponent;
  let fixture: ComponentFixture<AjoutDiscussionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AjoutDiscussionComponent]
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
