import { Component, OnInit } from '@angular/core';
import {DataService} from '../../services/data/data.service';
import {Router} from '@angular/router';
import {map} from 'rxjs/operators';

@Component({
  selector: 'app-create-run',
  templateUrl: './create-run.component.html',
  styleUrls: ['./create-run.component.css']
})
export class CreateRunComponent implements OnInit {
  tests: any;
  masterData: any;
  deviceTypes: [];
  selectedTests = [];
  selectedBiometricType: any;
  selectedDeviceType: any;
  selectedMdsVersion: any;
  selectedProcess: any;
  email = '';
  runName: '';

  constructor(private dataService: DataService, private router: Router) {
  }


  ngOnInit() {
    this.masterData = this.dataService.getMasterData().subscribe(
      masterData => this.masterData = masterData,
      error => window.alert(error)
    );

  }

  OnBiometricSelect(event) {
    this.deviceTypes = event.value.deviceType;
  }

  OnGetTestsClicked() {
    const requestBody = {
      biometricType: this.selectedBiometricType.type,
      deviceSubType: this.selectedDeviceType,
      mdsSpecificationVersion: this.selectedMdsVersion,
      process: this.selectedProcess
    };
    // console.log(requestBody);
    this.dataService.getTests(requestBody)
      .subscribe(
        tests => this.tests = tests,
        error => window.alert(error)
      );
  }

  OnCreateRunClicked() {
    const requestBody = {
      biometricType: this.selectedBiometricType.type,
      deviceSubType: this.selectedDeviceType,
      mdsSpecificationVersion: this.selectedMdsVersion,
      process: this.selectedProcess,
      tests: this.selectedTests,
      email: this.email,
      runName: this.runName
    };
    //console.log(this.selectedTests);
    this.dataService.createRun(requestBody)
      .pipe(
        map((body: any) => {
          return body.runId;
        })
      )
      .subscribe(
        runId => {
          window.alert('created. Run ID: ' + runId);
          this.router.navigate(['/']);
        },
        error => window.alert(error)
      );
  }

}
