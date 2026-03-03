import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { DiscussionDetailComponent } from './discussion-detail.component';

describe('DiscussionDetailComponent', () => {
  let component: DiscussionDetailComponent;
  let fixture: ComponentFixture<DiscussionDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DiscussionDetailComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DiscussionDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
