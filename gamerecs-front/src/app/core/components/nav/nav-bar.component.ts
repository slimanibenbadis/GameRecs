import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ButtonModule } from 'primeng/button';
import { MenubarModule } from 'primeng/menubar';
import { MenuItem } from 'primeng/api';
import { SearchModalComponent } from '../search/search-modal.component';
import { TooltipModule } from 'primeng/tooltip';

@Component({
  selector: 'app-nav-bar',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    ButtonModule, 
    MenubarModule,
    SearchModalComponent,
    TooltipModule
  ],
  templateUrl: './nav-bar.component.html',
  styleUrls: ['./nav-bar.component.css']
})
export class NavBarComponent implements OnInit, OnDestroy {
  @ViewChild('searchModal') searchModal!: SearchModalComponent;
  isAuthenticated = false;
  menuItems: MenuItem[] = [];
  private authSubscription?: Subscription;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    // Subscribe to authentication state changes
    this.authSubscription = this.authService.isAuthenticated$.subscribe(
      isAuth => {
        this.isAuthenticated = isAuth;
        this.initializeMenuItems();
      }
    );
  }

  ngOnDestroy() {
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
  }

  private initializeMenuItems() {
    this.menuItems = [
      {
        label: 'Dashboard',
        icon: 'pi pi-th-large',
        routerLink: '/dashboard',
        routerLinkActiveOptions: { exact: true }
      },
      {
        label: 'Game Library',
        icon: 'pi pi-book',
        routerLink: '/library'
      },
      {
        label: 'Recommendations',
        icon: 'pi pi-star',
        routerLink: ''
      },
      {
        label: 'Backlog',
        icon: 'pi pi-list',
        routerLink: ''
      },
      {
        label: 'Profile',
        icon: 'pi pi-user',
        routerLink: '/profile'
      }
    ];
  }

  openSearchModal() {
    this.searchModal.show();
  }

  logout() {
    // Log the user out
    this.authService.logout();
    
    // Redirect to the login page
    this.router.navigate(['/auth/login']);
    
    console.log('[NavBarComponent] User logged out, redirecting to login page');
  }
} 
