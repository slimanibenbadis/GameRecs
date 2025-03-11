import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ButtonModule } from 'primeng/button';
import { MenubarModule } from 'primeng/menubar';
import { MenuItem } from 'primeng/api';

@Component({
  selector: 'app-nav-bar',
  standalone: true,
  imports: [CommonModule, RouterModule, ButtonModule, MenubarModule],
  templateUrl: './nav-bar.component.html',
  styleUrls: ['./nav-bar.component.css']
})
export class NavBarComponent implements OnInit, OnDestroy {
  isAuthenticated = false;
  menuItems: MenuItem[] = [];
  private authSubscription?: Subscription;

  constructor(private authService: AuthService) {}

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
        label: 'Home',
        icon: 'pi pi-home',
        routerLink: '/',
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
        routerLink: '/recommendations'
      },
      {
        label: 'Backlog',
        icon: 'pi pi-list',
        routerLink: '/backlog'
      },
      {
        label: 'Profile',
        icon: 'pi pi-user',
        routerLink: '/profile'
      }
    ];
  }

  logout() {
    this.authService.logout();
  }
} 
